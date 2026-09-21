package com.kellyacademy.attendance.service;

import com.kellyacademy.attendance.dto.request.CrearAsistenciaRequest;
import com.kellyacademy.attendance.dto.response.AsistenciaResponse;
import com.kellyacademy.attendance.entity.Asistencia;
import com.kellyacademy.attendance.enums.EstadoAsistencia;
import com.kellyacademy.attendance.mapper.AsistenciaMapper;
import com.kellyacademy.attendance.repository.AsistenciaRepository;
import com.kellyacademy.course.entity.Clase;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.entity.Unidad;
import com.kellyacademy.course.repository.ClaseRepository;
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
class AsistenciaServiceTest {

    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private ClaseRepository claseRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private AsistenciaMapper asistenciaMapper;

    @InjectMocks private AsistenciaService asistenciaService;

    private UUID claseId;
    private UUID estudianteId;
    private UUID cursoId;
    private UUID docenteDuenoId;
    private Clase clase;
    private Usuario estudiante;
    private Usuario docente;

    @BeforeEach
    void setUp() {
        claseId = UUID.randomUUID();
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

        clase = new Clase();
        clase.setId(claseId);
        clase.setSemana(semana);
        clase.setTitulo("Clase 1");
        clase.setFechaHora(LocalDateTime.now().minusDays(1));

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

    private void autenticarComo(Usuario usuario) {
        CustomUserDetails userDetails = new CustomUserDetails(usuario);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // -------- crear --------

    @Test
    void crear_cuandoTodoValido_retornaAsistenciaResponse() {
        autenticarComo(docente);

        CrearAsistenciaRequest request = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );

        Asistencia guardada = new Asistencia();
        guardada.setId(UUID.randomUUID());

        when(claseRepository.findWithSemanaCursoDocenteById(claseId)).thenReturn(Optional.of(clase));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)).thenReturn(true);
        when(asistenciaRepository.findByClaseIdAndEstudianteId(claseId, estudianteId)).thenReturn(Optional.empty());
        when(asistenciaMapper.toEntity(request)).thenReturn(new Asistencia());
        when(asistenciaRepository.save(any(Asistencia.class))).thenReturn(guardada);
        when(asistenciaMapper.toResponse(guardada)).thenReturn(mock(AsistenciaResponse.class));

        AsistenciaResponse response = asistenciaService.crear(request);

        assertThat(response).isNotNull();
        verify(asistenciaRepository).save(any(Asistencia.class));
    }

    @Test
    void crear_cuandoClaseNoExiste_lanzaResourceNotFound() {
        autenticarComo(docente);

        CrearAsistenciaRequest request = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        when(claseRepository.findWithSemanaCursoDocenteById(claseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asistenciaService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_cuandoEstudianteNoExiste_lanzaResourceNotFound() {
        autenticarComo(docente);

        CrearAsistenciaRequest request = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        when(claseRepository.findWithSemanaCursoDocenteById(claseId)).thenReturn(Optional.of(clase));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asistenciaService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_cuandoEstudianteSinRolEstudiante_lanzaBusinessException() {
        autenticarComo(docente);

        estudiante.setRoles(Set.of());
        CrearAsistenciaRequest request = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        when(claseRepository.findWithSemanaCursoDocenteById(claseId)).thenReturn(Optional.of(clase));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));

        assertThatThrownBy(() -> asistenciaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ESTUDIANTE");
    }

    @Test
    void crear_cuandoEstudianteNoMatriculado_lanzaBusinessException() {
        autenticarComo(docente);

        CrearAsistenciaRequest request = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        when(claseRepository.findWithSemanaCursoDocenteById(claseId)).thenReturn(Optional.of(clase));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)).thenReturn(false);

        assertThatThrownBy(() -> asistenciaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("matriculado");
    }

    @Test
    void crear_cuandoClaseSinFecha_lanzaBusinessException() {
        autenticarComo(docente);

        clase.setFechaHora(null);
        CrearAsistenciaRequest request = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        when(claseRepository.findWithSemanaCursoDocenteById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> asistenciaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no tiene fecha");
    }

    @Test
    void crear_cuandoClaseNoImpartida_lanzaBusinessException() {
        autenticarComo(docente);

        clase.setFechaHora(LocalDateTime.now().plusDays(1));
        CrearAsistenciaRequest request = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        when(claseRepository.findWithSemanaCursoDocenteById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> asistenciaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no se ha impartido");
    }

    @Test
    void crear_cuandoAsistenciaDuplicada_lanzaBusinessException() {
        autenticarComo(docente);

        CrearAsistenciaRequest request = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        when(claseRepository.findWithSemanaCursoDocenteById(claseId)).thenReturn(Optional.of(clase));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)).thenReturn(true);
        when(asistenciaRepository.findByClaseIdAndEstudianteId(claseId, estudianteId))
                .thenReturn(Optional.of(new Asistencia()));

        assertThatThrownBy(() -> asistenciaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya existe asistencia");
    }

    @Test
    void crear_cuandoDocenteAjeno_lanzaAccessDenied() {
        Rol rolDocente = new Rol();
        rolDocente.setNombre("DOCENTE");
        Usuario otroDocente = new Usuario();
        otroDocente.setId(UUID.randomUUID());
        otroDocente.setRoles(Set.of(rolDocente));
        autenticarComo(otroDocente);

        CrearAsistenciaRequest request = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        when(claseRepository.findWithSemanaCursoDocenteById(claseId)).thenReturn(Optional.of(clase));

        assertThatThrownBy(() -> asistenciaService.crear(request))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}