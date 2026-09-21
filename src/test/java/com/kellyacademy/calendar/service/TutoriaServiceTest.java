package com.kellyacademy.calendar.service;

import com.kellyacademy.calendar.dto.request.ActualizarTutoriaRequest;
import com.kellyacademy.calendar.dto.request.CrearTutoriaRequest;
import com.kellyacademy.calendar.dto.response.TutoriaResponse;
import com.kellyacademy.calendar.entity.Tutoria;
import com.kellyacademy.calendar.enums.EstadoTutoria;
import com.kellyacademy.calendar.mapper.TutoriaMapper;
import com.kellyacademy.calendar.repository.TutoriaRepository;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
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
import org.springframework.security.access.AccessDeniedException;
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
class TutoriaServiceTest {

    @Mock private TutoriaRepository tutoriaRepository;
    @Mock private TutoriaMapper tutoriaMapper;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private MatriculaRepository matriculaRepository;

    @InjectMocks private TutoriaService tutoriaService;

    private UUID estudianteId;
    private UUID docenteId;
    private Usuario estudiante;
    private Usuario docente;

    @BeforeEach
    void setUp() {
        estudianteId = UUID.randomUUID();
        docenteId = UUID.randomUUID();

        Rol rolEstudiante = new Rol();
        rolEstudiante.setNombre("ESTUDIANTE");
        estudiante = new Usuario();
        estudiante.setId(estudianteId);
        estudiante.setNombre("Est");
        estudiante.setApellido("Uno");
        estudiante.setRoles(Set.of(rolEstudiante));

        Rol rolDocente = new Rol();
        rolDocente.setNombre("DOCENTE");
        docente = new Usuario();
        docente.setId(docenteId);
        docente.setNombre("Doc");
        docente.setApellido("Uno");
        docente.setRoles(Set.of(rolDocente));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Usuario usuario) {
        CustomUserDetails userDetails = new CustomUserDetails(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    private CrearTutoriaRequest requestValido(UUID cursoId, LocalDateTime fecha) {
        return new CrearTutoriaRequest(estudianteId, docenteId, cursoId, fecha, 60);
    }

    // -------- crear --------

    @Test
    void crear_comoEstudiante_sinCurso_ok() {
        autenticarComo(estudiante);

        CrearTutoriaRequest req = requestValido(null, LocalDateTime.now().plusDays(1));

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        when(tutoriaMapper.toEntity(req)).thenReturn(new Tutoria());
        when(tutoriaRepository.save(any(Tutoria.class))).thenAnswer(inv -> {
            Tutoria t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(any()))
                .thenAnswer(inv -> Optional.of(new Tutoria()));
        when(tutoriaMapper.toResponse(any(Tutoria.class))).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.crear(req);

        verify(tutoriaRepository).save(any(Tutoria.class));
    }

    @Test
    void crear_comoDocente_sinCurso_ok() {
        autenticarComo(docente);

        CrearTutoriaRequest req = requestValido(null, LocalDateTime.now().plusDays(1));

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        when(tutoriaMapper.toEntity(req)).thenReturn(new Tutoria());
        when(tutoriaRepository.save(any(Tutoria.class))).thenAnswer(inv -> {
            Tutoria t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(any()))
                .thenAnswer(inv -> Optional.of(new Tutoria()));
        when(tutoriaMapper.toResponse(any(Tutoria.class))).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.crear(req);

        verify(tutoriaRepository).save(any(Tutoria.class));
    }

    @Test
    void crear_comoTercero_lanzaAccessDenied() {
        Usuario tercero = new Usuario();
        tercero.setId(UUID.randomUUID());
        tercero.setRoles(Set.of());
        autenticarComo(tercero);

        CrearTutoriaRequest req = requestValido(null, LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> tutoriaService.crear(req))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(tutoriaRepository, usuarioRepository);
    }

    @Test
    void crear_fechaPasada_lanzaBusinessException() {
        autenticarComo(estudiante);

        CrearTutoriaRequest req = requestValido(null, LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> tutoriaService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("posterior");

        verifyNoInteractions(tutoriaRepository, usuarioRepository);
    }

    @Test
    void crear_estudianteSinRolEstudiante_lanzaBusinessException() {
        autenticarComo(docente);

        estudiante.setRoles(Set.of());
        CrearTutoriaRequest req = requestValido(null, LocalDateTime.now().plusDays(1));

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));

        assertThatThrownBy(() -> tutoriaService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ESTUDIANTE");
    }

    @Test
    void crear_conCurso_docenteNoEsDueno_lanzaBusinessException() {
        autenticarComo(estudiante);

        UUID cursoId = UUID.randomUUID();
        Curso curso = new Curso();
        curso.setId(cursoId);
        Usuario otroDocente = new Usuario();
        otroDocente.setId(UUID.randomUUID());
        curso.setDocente(otroDocente);

        CrearTutoriaRequest req = requestValido(cursoId, LocalDateTime.now().plusDays(1));

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));

        assertThatThrownBy(() -> tutoriaService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("docente dueno");
    }

    @Test
    void crear_conCurso_estudianteNoMatriculado_lanzaBusinessException() {
        autenticarComo(estudiante);

        UUID cursoId = UUID.randomUUID();
        Curso curso = new Curso();
        curso.setId(cursoId);
        curso.setDocente(docente);

        CrearTutoriaRequest req = requestValido(cursoId, LocalDateTime.now().plusDays(1));

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(matriculaRepository.findByCursoIdAndEstudianteId(cursoId, estudianteId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutoriaService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("matriculado");
    }

    @Test
    void crear_conCurso_ok() {
        autenticarComo(estudiante);

        UUID cursoId = UUID.randomUUID();
        Curso curso = new Curso();
        curso.setId(cursoId);
        curso.setDocente(docente);

        Matricula m = new Matricula();
        m.setEstado(EstadoMatricula.ACTIVA);

        CrearTutoriaRequest req = requestValido(cursoId, LocalDateTime.now().plusDays(1));

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(matriculaRepository.findByCursoIdAndEstudianteId(cursoId, estudianteId))
                .thenReturn(Optional.of(m));
        when(tutoriaMapper.toEntity(req)).thenReturn(new Tutoria());
        when(tutoriaRepository.save(any(Tutoria.class))).thenAnswer(inv -> {
            Tutoria t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(any()))
                .thenAnswer(inv -> Optional.of(new Tutoria()));
        when(tutoriaMapper.toResponse(any(Tutoria.class))).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.crear(req);

        verify(tutoriaRepository).save(any(Tutoria.class));
    }

    // -------- obtener --------

    @Test
    void obtener_inexistente_lanzaResourceNotFound() {
        autenticarComo(estudiante);
        UUID id = UUID.randomUUID();

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutoriaService.obtener(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtener_comoTercero_lanzaAccessDenied() {
        Usuario tercero = new Usuario();
        tercero.setId(UUID.randomUUID());
        tercero.setRoles(Set.of());
        autenticarComo(tercero);

        Tutoria t = new Tutoria();
        t.setId(UUID.randomUUID());
        t.setEstudiante(estudiante);
        t.setDocente(docente);

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));

        assertThatThrownBy(() -> tutoriaService.obtener(t.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // -------- actualizar --------

    @Test
    void actualizar_pendiente_cambiaFecha_ok() {
        autenticarComo(estudiante);

        Tutoria t = new Tutoria();
        t.setId(UUID.randomUUID());
        t.setEstudiante(estudiante);
        t.setDocente(docente);
        t.setEstado(EstadoTutoria.PENDIENTE);
        t.setFecha(LocalDateTime.now().plusDays(1));
        t.setDuracionMinutos(60);

        ActualizarTutoriaRequest req = new ActualizarTutoriaRequest(
                LocalDateTime.now().plusDays(2), 90, "nueva nota"
        );

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaRepository.save(t)).thenReturn(t);
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaMapper.toResponse(t)).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.actualizar(t.getId(), req);

        verify(tutoriaMapper).actualizarDesdeRequest(req, t);
    }

    @Test
    void actualizar_confirmada_cambiaFecha_lanzaBusinessException() {
        autenticarComo(estudiante);

        Tutoria t = new Tutoria();
        t.setId(UUID.randomUUID());
        t.setEstudiante(estudiante);
        t.setDocente(docente);
        t.setEstado(EstadoTutoria.CONFIRMADA);
        t.setFecha(LocalDateTime.now().plusDays(1));
        t.setDuracionMinutos(60);

        ActualizarTutoriaRequest req = new ActualizarTutoriaRequest(
                LocalDateTime.now().plusDays(5), 60, null
        );

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));

        assertThatThrownBy(() -> tutoriaService.actualizar(t.getId(), req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDIENTE");
    }

    @Test
    void actualizar_confirmada_soloNotas_ok() {
        autenticarComo(estudiante);

        LocalDateTime fechaFija = LocalDateTime.now().plusDays(1);
        Tutoria t = new Tutoria();
        t.setId(UUID.randomUUID());
        t.setEstudiante(estudiante);
        t.setDocente(docente);
        t.setEstado(EstadoTutoria.CONFIRMADA);
        t.setFecha(fechaFija);
        t.setDuracionMinutos(60);

        // Misma fecha y duracion: solo cambia notas.
        ActualizarTutoriaRequest req = new ActualizarTutoriaRequest(fechaFija, 60, "nota editada");

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaRepository.save(t)).thenReturn(t);
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaMapper.toResponse(t)).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.actualizar(t.getId(), req);

        verify(tutoriaMapper).actualizarDesdeRequest(req, t);
    }

    @Test
    void actualizar_fechaPasada_lanzaBusinessException() {
        autenticarComo(estudiante);

        Tutoria t = new Tutoria();
        t.setId(UUID.randomUUID());
        t.setEstudiante(estudiante);
        t.setDocente(docente);
        t.setEstado(EstadoTutoria.PENDIENTE);
        t.setFecha(LocalDateTime.now().plusDays(1));
        t.setDuracionMinutos(60);

        ActualizarTutoriaRequest req = new ActualizarTutoriaRequest(
                LocalDateTime.now().minusDays(1), 60, null
        );

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));

        assertThatThrownBy(() -> tutoriaService.actualizar(t.getId(), req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    void actualizar_comoTercero_lanzaAccessDenied() {
        Usuario tercero = new Usuario();
        tercero.setId(UUID.randomUUID());
        tercero.setRoles(Set.of());
        autenticarComo(tercero);

        Tutoria t = new Tutoria();
        t.setId(UUID.randomUUID());
        t.setEstudiante(estudiante);
        t.setDocente(docente);
        t.setEstado(EstadoTutoria.PENDIENTE);

        ActualizarTutoriaRequest req = new ActualizarTutoriaRequest(
                LocalDateTime.now().plusDays(2), 60, null
        );

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));

        assertThatThrownBy(() -> tutoriaService.actualizar(t.getId(), req))
                .isInstanceOf(AccessDeniedException.class);
    }

    // -------- eliminar --------

    @Test
    void eliminar_existente_ok() {
        autenticarComo(docente);

        Tutoria t = new Tutoria();
        t.setId(UUID.randomUUID());

        when(tutoriaRepository.findById(t.getId())).thenReturn(Optional.of(t));

        tutoriaService.eliminar(t.getId());

        verify(tutoriaRepository).delete(t);
    }
}