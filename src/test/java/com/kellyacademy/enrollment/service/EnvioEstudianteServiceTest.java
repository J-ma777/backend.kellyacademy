package com.kellyacademy.enrollment.service;

import com.kellyacademy.course.entity.Tarea;
import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.course.service.AccesoCursoService;
import com.kellyacademy.enrollment.dto.request.EnviarEntregaRequest;
import com.kellyacademy.enrollment.entity.Entrega;
import com.kellyacademy.enrollment.enums.EstadoEntrega;
import com.kellyacademy.enrollment.mapper.EntregaMapper;
import com.kellyacademy.enrollment.repository.EntregaRepository;
import com.kellyacademy.storage.ArchivoAccesoService;
import com.kellyacademy.support.TestData;
import com.kellyacademy.shared.config.AppTime;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EnvioEstudianteServiceTest {
    @Mock TareaRepository tareas;
    @Mock EntregaRepository entregas;
    @Mock UsuarioRepository usuarios;
    @Mock EntregaMapper mapper;
    @Mock AccesoCursoService acceso;
    @Mock ArchivoAccesoService archivos;
    @InjectMocks EnvioEstudianteService service;
    private Tarea tarea; private Usuario alumno;
    @BeforeEach void setup() {
        alumno = TestData.usuario("ESTUDIANTE"); TestData.autenticar(alumno);
        tarea = TestData.tarea(TestData.curso(TestData.usuario("DOCENTE")));
        when(tareas.findWithSemanaCursoDocenteById(tarea.getId())).thenReturn(Optional.of(tarea));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    private EnviarEntregaRequest request(String file, String text) { return new EnviarEntregaRequest(tarea.getId(), file, text); }
    @Test void persisteTextoNormalizadoYUsuarioAutenticado() {
        when(usuarios.getReferenceById(alumno.getId())).thenReturn(alumno);
        service.enviar(request(null, "  Mi respuesta  "));
        var saved = ArgumentCaptor.forClass(Entrega.class); verify(entregas).save(saved.capture());
        assertThat(saved.getValue().getTextoRespuesta()).isEqualTo("Mi respuesta");
        assertThat(saved.getValue().getEstudiante()).isSameAs(alumno);
        assertThat(saved.getValue().getTarea()).isSameAs(tarea);
        assertThat(saved.getValue().getEstado()).isEqualTo(EstadoEntrega.PENDIENTE);
    }
    @Test void envioFueraDePlazoSeRegistraTardeYConservaLaEntregaExistente() {
        tarea.setFechaLimite(LocalDateTime.now(AppTime.ZONA_NEGOCIO).minusDays(1));
        var anterior = new Entrega(); anterior.setId(java.util.UUID.randomUUID());
        when(entregas.findByTareaIdAndEstudianteId(tarea.getId(), alumno.getId())).thenReturn(Optional.of(anterior));
        when(usuarios.getReferenceById(alumno.getId())).thenReturn(alumno);
        service.enviar(request("https://example.org/respuesta.pdf", "Respuesta actualizada"));
        verify(entregas).save(anterior); assertThat(anterior.getEstado()).isEqualTo(EstadoEntrega.TARDE);
        assertThat(anterior.getUrlArchivo()).isEqualTo("https://example.org/respuesta.pdf");
    }
    @Test void entregaCalificadaNoPuedeSerSobrescrita() {
        var anterior = new Entrega(); anterior.setEstado(EstadoEntrega.CALIFICADA);
        when(entregas.findByTareaIdAndEstudianteId(tarea.getId(), alumno.getId())).thenReturn(Optional.of(anterior));
        assertThatThrownBy(() -> service.enviar(request(null, "Reemplazo"))).isInstanceOf(BusinessException.class).hasMessageContaining("calificada");
        verify(entregas, never()).save(any());
    }
    @Test void rechazaRespuestaVaciaYEnlacesEjecutablesAntesDePersistir() {
        assertThatThrownBy(() -> service.enviar(request("  ", "  "))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.enviar(request("javascript:alert(1)", "Respuesta"))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(entregas, archivos);
    }
    @Test void faltaDeMatriculaBloqueaElEnvioAntesDeConsultarArchivosOEntregas() {
        doThrow(new AccessDeniedException("Sin matrícula")).when(acceso).validarEstudianteActivo(any());
        assertThatThrownBy(() -> service.enviar(request(null, "Respuesta"))).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(entregas, archivos, usuarios);
    }
}
