package com.kellyacademy.course.service;

import com.kellyacademy.course.entity.*;
import com.kellyacademy.course.repository.*;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.storage.ArchivoAccesoService;
import com.kellyacademy.user.repository.UsuarioRepository;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.support.TestData;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ContenidoCursoServiceTest {
    @Mock AccesoCursoService acceso; @Mock ArchivoAccesoService archivos;
    @Mock SemanaRepository semanas; @Mock MaterialRepository materiales; @Mock ClaseRepository clases; @Mock TareaRepository tareas;
    @Mock MaterialService materialService; @Mock ClaseService claseService; @Mock TareaService tareaService;
    @Mock ProgresoContenidoRepository progresos; @Mock UsuarioRepository usuarios; @Mock MatriculaRepository matriculas; @Mock Validator validator;
    @InjectMocks ContenidoCursoService service;
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    private Material material() {
        var m = new Material(); m.setId(UUID.randomUUID()); m.setSemana(TestData.tarea(TestData.curso(TestData.usuario("DOCENTE"))).getSemana());
        when(materiales.findWithSemanaCursoDocenteById(m.getId())).thenReturn(Optional.of(m)); return m;
    }
    @Test void recursoAjenoNoSePuedePublicarDesdeOtroCurso() {
        var m = material();
        assertThatThrownBy(() -> service.publicar(UUID.randomUUID(), "MATERIAL", m.getId(), true, true)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(acceso); assertThat(m.getPublicado()).isTrue();
    }
    @Test void publicacionValidaPermisosYActualizaVisibilidad() {
        var m = material(); var curso = m.getSemana().getUnidad().getCurso();
        service.publicar(curso.getId(), "MATERIAL", m.getId(), true, false);
        verify(acceso).validarGestor(curso); assertThat(m.getVisible()).isFalse();
    }
    @Test void completarDosVecesNoDuplicaProgreso() {
        var alumno = TestData.usuario("ESTUDIANTE"); TestData.autenticar(alumno); var m = material();
        when(progresos.existsByEstudianteIdAndMaterialId(alumno.getId(), m.getId())).thenReturn(true);
        service.completar(m.getSemana().getUnidad().getCurso().getId(), "MATERIAL", m.getId());
        verify(progresos, never()).save(any());
    }
    @Test void completarPrimeraVezVinculaAlumnoYMaterialCorrectos() {
        var alumno = TestData.usuario("ESTUDIANTE"); TestData.autenticar(alumno); var m = material();
        when(usuarios.getReferenceById(alumno.getId())).thenReturn(alumno);
        service.completar(m.getSemana().getUnidad().getCurso().getId(), "MATERIAL", m.getId());
        var saved = ArgumentCaptor.forClass(ProgresoContenido.class); verify(progresos).save(saved.capture());
        assertThat(saved.getValue().getEstudiante()).isSameAs(alumno); assertThat(saved.getValue().getMaterial()).isSameAs(m); assertThat(saved.getValue().getClase()).isNull();
    }
    @Test void actividadRequiereEntregaYNoPuedeMarcarseComoMaterialLeido() {
        TestData.autenticar(TestData.usuario("ESTUDIANTE")); var task = TestData.tarea(TestData.curso(TestData.usuario("DOCENTE")));
        when(tareas.findWithSemanaCursoDocenteById(task.getId())).thenReturn(Optional.of(task));
        assertThatThrownBy(() -> service.completar(task.getSemana().getUnidad().getCurso().getId(), "TASK", task.getId())).isInstanceOf(BusinessException.class).hasMessageContaining("entrega");
        verifyNoInteractions(progresos);
    }
    @Test void matriculaCompletadaConservaSuHistorialAlIntentarRetirarla() {
        var alumno = TestData.usuario("ESTUDIANTE"); var curso = TestData.curso(TestData.usuario("DOCENTE"));
        var m = TestData.matricula(curso, alumno, EstadoMatricula.COMPLETADA);
        when(acceso.obtener(curso.getId())).thenReturn(curso); when(matriculas.findByCursoIdAndEstudianteId(curso.getId(), alumno.getId())).thenReturn(Optional.of(m));
        assertThatThrownBy(() -> service.retirarAlumno(curso.getId(), alumno.getId())).isInstanceOf(BusinessException.class);
        assertThat(m.getEstado()).isEqualTo(EstadoMatricula.COMPLETADA);
    }
}
