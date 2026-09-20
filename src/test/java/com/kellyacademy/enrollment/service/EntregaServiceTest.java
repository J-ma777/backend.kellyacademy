package com.kellyacademy.enrollment.service;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.entity.Tarea;
import com.kellyacademy.course.entity.Unidad;
import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.enrollment.dto.request.ActualizarEntregaRequest;
import com.kellyacademy.enrollment.dto.request.CrearEntregaRequest;
import com.kellyacademy.enrollment.dto.response.EntregaResponse;
import com.kellyacademy.enrollment.entity.Entrega;
import com.kellyacademy.enrollment.enums.EstadoEntrega;
import com.kellyacademy.enrollment.mapper.EntregaMapper;
import com.kellyacademy.enrollment.repository.EntregaRepository;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.security.user.CustomUserDetails;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.user.entity.Rol;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntregaServiceTest {

    @Mock private EntregaRepository entregaRepository;
    @Mock private TareaRepository tareaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private EntregaMapper entregaMapper;

    @InjectMocks private EntregaService entregaService;

    private UUID tareaId;
    private UUID estudianteId;
    private UUID cursoId;
    private UUID docenteDuenoId;
    private Tarea tarea;
    private Usuario estudiante;
    private Usuario docente;

    @BeforeEach
    void setUp() {
        tareaId = UUID.randomUUID();
        estudianteId = UUID.randomUUID();
        cursoId = UUID.randomUUID();
        docenteDuenoId = UUID.randomUUID();

        Rol rolDocente = new Rol();
        rolDocente.setNombre("DOCENTE");

        docente = new Usuario();
        docente.setId(docenteDuenoId);
        docente.setRoles(Set.of(rolDocente));

        Curso curso = new Curso();
        curso.setId(cursoId);
        curso.setDocente(docente);

        Unidad unidad = new Unidad();
        unidad.setCurso(curso);

        Semana semana = new Semana();
        semana.setUnidad(unidad);

        tarea = new Tarea();
        tarea.setId(tareaId);
        tarea.setSemana(semana);
        tarea.setFechaLimite(LocalDateTime.now().plusDays(7));

        Rol rolEstudiante = new Rol();
        rolEstudiante.setNombre("ESTUDIANTE");

        estudiante = new Usuario();
        estudiante.setId(estudianteId);
        estudiante.setRoles(Set.of(rolEstudiante));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Simula autenticacion de un usuario en el SecurityContext.
    // Necesario porque EntregaService delega la autorizacion a SecurityUtils,
    // que revienta con IllegalStateException si no hay contexto.
    private void autenticarComo(Usuario usuario) {
        CustomUserDetails userDetails = new CustomUserDetails(usuario);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // -------- crear --------

    @Test
    void crear_cuandoTodoValido_retornaEntregaResponse() {
        autenticarComo(docente);

        CrearEntregaRequest request = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        Entrega guardada = new Entrega();
        guardada.setId(UUID.randomUUID());

        when(tareaRepository.findWithSemanaCursoDocenteById(tareaId)).thenReturn(Optional.of(tarea));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)).thenReturn(true);
        when(entregaRepository.findByTareaIdAndEstudianteId(tareaId, estudianteId)).thenReturn(Optional.empty());
        when(entregaMapper.toEntity(request)).thenReturn(new Entrega());
        when(entregaRepository.save(any(Entrega.class))).thenReturn(guardada);
        when(entregaRepository.findWithTareaAndEstudianteById(guardada.getId()))
                .thenReturn(Optional.of(guardada));
        when(entregaMapper.toResponse(guardada)).thenReturn(mock(EntregaResponse.class));

        EntregaResponse response = entregaService.crear(request);

        assertThat(response).isNotNull();
        verify(entregaRepository).save(any(Entrega.class));
    }

    @Test
    void crear_cuandoTareaNoExiste_lanzaResourceNotFound() {
        autenticarComo(docente);

        CrearEntregaRequest request = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        when(tareaRepository.findWithSemanaCursoDocenteById(tareaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entregaService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_cuandoEstudianteNoExiste_lanzaResourceNotFound() {
        autenticarComo(docente);

        CrearEntregaRequest request = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        when(tareaRepository.findWithSemanaCursoDocenteById(tareaId)).thenReturn(Optional.of(tarea));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entregaService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_cuandoEstudianteSinRolEstudiante_lanzaBusinessException() {
        autenticarComo(docente);

        estudiante.setRoles(Set.of());
        CrearEntregaRequest request = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        when(tareaRepository.findWithSemanaCursoDocenteById(tareaId)).thenReturn(Optional.of(tarea));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));

        assertThatThrownBy(() -> entregaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ESTUDIANTE");
    }

    @Test
    void crear_cuandoEstudianteNoMatriculado_lanzaBusinessException() {
        autenticarComo(docente);

        CrearEntregaRequest request = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        when(tareaRepository.findWithSemanaCursoDocenteById(tareaId)).thenReturn(Optional.of(tarea));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)).thenReturn(false);

        assertThatThrownBy(() -> entregaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("matriculado");
    }

    @Test
    void crear_cuandoEntregaDuplicada_lanzaBusinessException() {
        autenticarComo(docente);

        CrearEntregaRequest request = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        when(tareaRepository.findWithSemanaCursoDocenteById(tareaId)).thenReturn(Optional.of(tarea));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)).thenReturn(true);
        when(entregaRepository.findByTareaIdAndEstudianteId(tareaId, estudianteId))
                .thenReturn(Optional.of(new Entrega()));

        assertThatThrownBy(() -> entregaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya existe una entrega");
    }

    // -------- actualizar --------

    @Test
    void actualizar_cuandoEntregaCalificada_lanzaBusinessException() {
        // Autenticamos como el estudiante dueno: es quien tiene permitido actualizar.
        autenticarComo(estudiante);

        ActualizarEntregaRequest request = new ActualizarEntregaRequest("https://example.com/nuevo.pdf");
        Entrega entrega = new Entrega();
        entrega.setEstado(EstadoEntrega.CALIFICADA);
        entrega.setEstudiante(estudiante);
        entrega.setTarea(tarea);

        when(entregaRepository.findWithTareaAndEstudianteById(any())).thenReturn(Optional.of(entrega));

        assertThatThrownBy(() -> entregaService.actualizar(UUID.randomUUID(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("calificada");
    }
}