package com.kellyacademy.calendar.service;

import com.kellyacademy.calendar.dto.request.ActualizarTutoriaRequest;
import com.kellyacademy.calendar.dto.request.CrearTutoriaRequest;
import com.kellyacademy.calendar.dto.response.TutoriaResponse;
import com.kellyacademy.calendar.entity.DisponibilidadTutoria;
import com.kellyacademy.calendar.entity.Tutoria;
import com.kellyacademy.calendar.enums.EstadoTutoria;
import com.kellyacademy.calendar.mapper.TutoriaMapper;
import com.kellyacademy.calendar.repository.DisponibilidadTutoriaRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TutoriaServiceTest {

    @Mock private TutoriaRepository tutoriaRepository;
    @Mock private DisponibilidadTutoriaRepository disponibilidadTutoriaRepository;
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

    private DisponibilidadTutoria bloqueValido(DayOfWeek diaSemana, LocalTime inicio, LocalTime fin, boolean bloqueada) {
        DisponibilidadTutoria d = new DisponibilidadTutoria();
        d.setId(UUID.randomUUID());
        d.setDocente(docente);
        d.setDiaSemana(diaSemana);
        d.setHoraInicio(inicio);
        d.setHoraFin(fin);
        d.setBloqueada(bloqueada);
        return d;
    }

    private void mockDisponibilidadValidaYNoSolape() {
        when(disponibilidadTutoriaRepository.findByDocenteIdAndDiaSemana(any(), any()))
                .thenReturn(List.of(bloqueValido(DayOfWeek.MONDAY, LocalTime.MIN, LocalTime.MAX, false)));
        when(tutoriaRepository.findConfirmadasCandidatasSolape(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
    }

    // -------- crear --------

    @Test
    void crear_comoEstudiante_sinCurso_ok() {
        autenticarComo(estudiante);

        CrearTutoriaRequest req = requestValido(null, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        mockDisponibilidadValidaYNoSolape();
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

        CrearTutoriaRequest req = requestValido(null, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        mockDisponibilidadValidaYNoSolape();
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

        CrearTutoriaRequest req = requestValido(null, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

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
        CrearTutoriaRequest req = requestValido(null, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

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

        CrearTutoriaRequest req = requestValido(cursoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

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

        CrearTutoriaRequest req = requestValido(cursoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

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

        CrearTutoriaRequest req = requestValido(cursoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(matriculaRepository.findByCursoIdAndEstudianteId(cursoId, estudianteId))
                .thenReturn(Optional.of(m));
        mockDisponibilidadValidaYNoSolape();
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

    // -------- tests #47 + #48 en crear --------

    @Test
    void crear_cuandoDocenteSinDisponibilidad_lanzaBusinessException() {
        autenticarComo(estudiante);
        LocalDateTime fecha = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        CrearTutoriaRequest req = requestValido(null, fecha);

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        when(disponibilidadTutoriaRepository.findByDocenteIdAndDiaSemana(eq(docenteId), eq(fecha.getDayOfWeek())))
                .thenReturn(List.of());

        assertThatThrownBy(() -> tutoriaService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disponibilidad");

        verify(tutoriaRepository, never()).save(any());
    }

    @Test
    void crear_cuandoBloqueBloqueado_lanzaBusinessException() {
        autenticarComo(estudiante);
        LocalDateTime fecha = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        CrearTutoriaRequest req = requestValido(null, fecha);

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        when(disponibilidadTutoriaRepository.findByDocenteIdAndDiaSemana(eq(docenteId), eq(fecha.getDayOfWeek())))
                .thenReturn(List.of(bloqueValido(fecha.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(12, 0), true)));

        assertThatThrownBy(() -> tutoriaService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bloquead");

        verify(tutoriaRepository, never()).save(any());
    }

    @Test
    void crear_cuandoBloqueValidoNoBloqueado_ok() {
        autenticarComo(estudiante);
        LocalDateTime fecha = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        CrearTutoriaRequest req = requestValido(null, fecha);

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        DisponibilidadTutoria bloqueBloqueado = bloqueValido(fecha.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(12, 0), true);
        DisponibilidadTutoria bloqueDisponible = bloqueValido(fecha.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(13, 0), false);
        when(disponibilidadTutoriaRepository.findByDocenteIdAndDiaSemana(eq(docenteId), eq(fecha.getDayOfWeek())))
                .thenReturn(List.of(bloqueBloqueado, bloqueDisponible));
        when(tutoriaRepository.findConfirmadasCandidatasSolape(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(tutoriaMapper.toEntity(req)).thenReturn(new Tutoria());
        when(tutoriaRepository.save(any(Tutoria.class))).thenAnswer(inv -> {
            Tutoria t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(any()))
                .thenReturn(Optional.of(new Tutoria()));
        when(tutoriaMapper.toResponse(any(Tutoria.class))).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.crear(req);

        verify(tutoriaRepository).save(any(Tutoria.class));
    }

    @Test
    void crear_cuandoCruzaMedianoche_lanzaBusinessException() {
        autenticarComo(estudiante);
        LocalDateTime fecha = LocalDateTime.now().plusDays(1).withHour(23).withMinute(30);
        CrearTutoriaRequest req = new CrearTutoriaRequest(estudianteId, docenteId, null, fecha, 60);

        assertThatThrownBy(() -> tutoriaService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("medianoche");

        verifyNoInteractions(disponibilidadTutoriaRepository, tutoriaRepository);
    }

    @Test
    void crear_cuandoSolapeConConfirmadaDelDocente_lanzaBusinessException() {
        autenticarComo(estudiante);
        LocalDateTime fecha = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        CrearTutoriaRequest req = requestValido(null, fecha);

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        when(disponibilidadTutoriaRepository.findByDocenteIdAndDiaSemana(eq(docenteId), eq(fecha.getDayOfWeek())))
                .thenReturn(List.of(bloqueValido(fecha.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(12, 0), false)));

        Tutoria confDocente = new Tutoria();
        confDocente.setId(UUID.randomUUID());
        confDocente.setDocente(docente);
        confDocente.setEstudiante(new Usuario());
        confDocente.setFecha(fecha.plusMinutes(30)); // 10:30 a 11:30 solapa con 10:00 a 11:00
        confDocente.setDuracionMinutos(60);
        confDocente.setEstado(EstadoTutoria.CONFIRMADA);

        when(tutoriaRepository.findConfirmadasCandidatasSolape(eq(docenteId), eq(estudianteId), any(), any(), isNull()))
                .thenReturn(List.of(confDocente));

        assertThatThrownBy(() -> tutoriaService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("solapa");

        verify(tutoriaRepository, never()).save(any());
    }

    @Test
    void crear_cuandoSolapeConConfirmadaDelEstudiante_lanzaBusinessException() {
        autenticarComo(estudiante);
        LocalDateTime fecha = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        CrearTutoriaRequest req = requestValido(null, fecha);

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        when(disponibilidadTutoriaRepository.findByDocenteIdAndDiaSemana(eq(docenteId), eq(fecha.getDayOfWeek())))
                .thenReturn(List.of(bloqueValido(fecha.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(12, 0), false)));

        Tutoria confEstudiante = new Tutoria();
        confEstudiante.setId(UUID.randomUUID());
        confEstudiante.setDocente(new Usuario());
        confEstudiante.setEstudiante(estudiante);
        confEstudiante.setFecha(fecha.minusMinutes(30)); // 09:30 a 10:30 solapa con 10:00 a 11:00
        confEstudiante.setDuracionMinutos(60);
        confEstudiante.setEstado(EstadoTutoria.CONFIRMADA);

        when(tutoriaRepository.findConfirmadasCandidatasSolape(eq(docenteId), eq(estudianteId), any(), any(), isNull()))
                .thenReturn(List.of(confEstudiante));

        assertThatThrownBy(() -> tutoriaService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("solapa");

        verify(tutoriaRepository, never()).save(any());
    }

    @Test
    void crear_cuandoOtraTutoriaEsPendiente_noFalla() {
        autenticarComo(estudiante);
        LocalDateTime fecha = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        CrearTutoriaRequest req = requestValido(null, fecha);

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        when(disponibilidadTutoriaRepository.findByDocenteIdAndDiaSemana(eq(docenteId), eq(fecha.getDayOfWeek())))
                .thenReturn(List.of(bloqueValido(fecha.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(12, 0), false)));
        when(tutoriaRepository.findConfirmadasCandidatasSolape(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(tutoriaMapper.toEntity(req)).thenReturn(new Tutoria());
        when(tutoriaRepository.save(any(Tutoria.class))).thenAnswer(inv -> {
            Tutoria t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(any()))
                .thenReturn(Optional.of(new Tutoria()));
        when(tutoriaMapper.toResponse(any(Tutoria.class))).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.crear(req);

        verify(tutoriaRepository).save(any(Tutoria.class));
    }

    @Test
    void crear_cuandoTutoriaAdyacente_noFalla() {
        autenticarComo(estudiante);
        LocalDateTime fecha = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        CrearTutoriaRequest req = requestValido(null, fecha); // 10:00 a 11:00

        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        when(disponibilidadTutoriaRepository.findByDocenteIdAndDiaSemana(eq(docenteId), eq(fecha.getDayOfWeek())))
                .thenReturn(List.of(bloqueValido(fecha.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(13, 0), false)));

        Tutoria anterior = new Tutoria();
        anterior.setId(UUID.randomUUID());
        anterior.setDocente(docente);
        anterior.setEstudiante(new Usuario());
        anterior.setFecha(fecha.minusMinutes(60)); // 09:00 a 10:00 (termina exactamente cuando empieza la nueva)
        anterior.setDuracionMinutos(60);
        anterior.setEstado(EstadoTutoria.CONFIRMADA);

        Tutoria posterior = new Tutoria();
        posterior.setId(UUID.randomUUID());
        posterior.setDocente(docente);
        posterior.setEstudiante(new Usuario());
        posterior.setFecha(fecha.plusMinutes(60)); // 11:00 a 12:00 (empieza exactamente cuando termina la nueva)
        posterior.setDuracionMinutos(60);
        posterior.setEstado(EstadoTutoria.CONFIRMADA);

        when(tutoriaRepository.findConfirmadasCandidatasSolape(any(), any(), any(), any(), any()))
                .thenReturn(List.of(anterior, posterior));
        when(tutoriaMapper.toEntity(req)).thenReturn(new Tutoria());
        when(tutoriaRepository.save(any(Tutoria.class))).thenAnswer(inv -> {
            Tutoria t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(any()))
                .thenReturn(Optional.of(new Tutoria()));
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
        t.setFecha(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        t.setDuracionMinutos(60);

        LocalDateTime nuevaFecha = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0);
        ActualizarTutoriaRequest req = new ActualizarTutoriaRequest(
                nuevaFecha, 90, "nueva nota"
        );

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        mockDisponibilidadValidaYNoSolape();
        when(tutoriaRepository.save(t)).thenReturn(t);
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaMapper.toResponse(t)).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.actualizar(t.getId(), req);

        verify(tutoriaMapper).actualizarDesdeRequest(req, t);
    }

    @Test
    void actualizar_cuandoSoloCambiaNotas_noRevalida() {
        autenticarComo(estudiante);

        LocalDateTime fechaFija = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        Tutoria t = new Tutoria();
        t.setId(UUID.randomUUID());
        t.setEstudiante(estudiante);
        t.setDocente(docente);
        t.setEstado(EstadoTutoria.PENDIENTE);
        t.setFecha(fechaFija);
        t.setDuracionMinutos(60);

        ActualizarTutoriaRequest req = new ActualizarTutoriaRequest(fechaFija, 60, "nueva nota");

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaRepository.save(t)).thenReturn(t);
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaMapper.toResponse(t)).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.actualizar(t.getId(), req);

        verify(disponibilidadTutoriaRepository, never()).findByDocenteIdAndDiaSemana(any(), any());
        verify(tutoriaRepository, never()).findConfirmadasCandidatasSolape(any(), any(), any(), any(), any());
    }

    @Test
    void actualizar_cuandoCambiaFecha_revalidaDisponibilidad() {
        autenticarComo(estudiante);

        LocalDateTime fechaOriginal = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime fechaNueva = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0);

        Tutoria t = new Tutoria();
        t.setId(UUID.randomUUID());
        t.setEstudiante(estudiante);
        t.setDocente(docente);
        t.setEstado(EstadoTutoria.PENDIENTE);
        t.setFecha(fechaOriginal);
        t.setDuracionMinutos(60);

        ActualizarTutoriaRequest req = new ActualizarTutoriaRequest(fechaNueva, 60, null);

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(disponibilidadTutoriaRepository.findByDocenteIdAndDiaSemana(eq(docenteId), eq(fechaNueva.getDayOfWeek())))
                .thenReturn(List.of()); // Sin disponibilidad en fechaNueva

        assertThatThrownBy(() -> tutoriaService.actualizar(t.getId(), req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disponibilidad");

        verify(tutoriaRepository, never()).save(any());
    }

    @Test
    void actualizar_confirmada_cambiaFecha_lanzaBusinessException() {
        autenticarComo(estudiante);

        Tutoria t = new Tutoria();
        t.setId(UUID.randomUUID());
        t.setEstudiante(estudiante);
        t.setDocente(docente);
        t.setEstado(EstadoTutoria.CONFIRMADA);
        t.setFecha(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        t.setDuracionMinutos(60);

        ActualizarTutoriaRequest req = new ActualizarTutoriaRequest(
                LocalDateTime.now().plusDays(5).withHour(10).withMinute(0), 60, null
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

        LocalDateTime fechaFija = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
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
        t.setFecha(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
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
                LocalDateTime.now().plusDays(2).withHour(10).withMinute(0), 60, null
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

    // ------------------------------------------------------------------
    // cambiarEstado
    // ------------------------------------------------------------------

    private Tutoria tutoriaConEstado(EstadoTutoria estado) {
        Tutoria t = new Tutoria();
        t.setId(UUID.randomUUID());
        t.setEstudiante(estudiante);
        t.setDocente(docente);
        t.setEstado(estado);
        t.setFecha(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        t.setDuracionMinutos(60);
        return t;
    }

    @Test
    void cambiarEstado_tutoriaInexistente_lanzaResourceNotFound() {
        autenticarComo(docente);
        UUID id = UUID.randomUUID();

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutoriaService.cambiarEstado(id, EstadoTutoria.CONFIRMADA))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cambiarEstado_pendienteAConfirmada_comoEstudiante_ok() {
        autenticarComo(estudiante);
        Tutoria t = tutoriaConEstado(EstadoTutoria.PENDIENTE);

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        mockDisponibilidadValidaYNoSolape();
        when(tutoriaRepository.save(t)).thenReturn(t);
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaMapper.toResponse(t)).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.cambiarEstado(t.getId(), EstadoTutoria.CONFIRMADA);

        assertThat(t.getEstado()).isEqualTo(EstadoTutoria.CONFIRMADA);
    }

    @Test
    void cambiarEstado_pendienteAConfirmada_comoDocente_ok() {
        autenticarComo(docente);
        Tutoria t = tutoriaConEstado(EstadoTutoria.PENDIENTE);

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        mockDisponibilidadValidaYNoSolape();
        when(tutoriaRepository.save(t)).thenReturn(t);
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaMapper.toResponse(t)).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.cambiarEstado(t.getId(), EstadoTutoria.CONFIRMADA);

        assertThat(t.getEstado()).isEqualTo(EstadoTutoria.CONFIRMADA);
    }

    @Test
    void cambiarEstado_aConfirmada_conSolape_lanzaBusinessException() {
        autenticarComo(docente);
        LocalDateTime fecha = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        Tutoria t = tutoriaConEstado(EstadoTutoria.PENDIENTE);
        t.setFecha(fecha);
        t.setDuracionMinutos(60);

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(disponibilidadTutoriaRepository.findByDocenteIdAndDiaSemana(eq(docenteId), any()))
                .thenReturn(List.of(bloqueValido(fecha.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(12, 0), false)));

        Tutoria solapada = new Tutoria();
        solapada.setId(UUID.randomUUID());
        solapada.setDocente(docente);
        solapada.setEstudiante(new Usuario());
        solapada.setFecha(fecha);
        solapada.setDuracionMinutos(60);
        solapada.setEstado(EstadoTutoria.CONFIRMADA);

        when(tutoriaRepository.findConfirmadasCandidatasSolape(eq(docenteId), eq(estudianteId), any(), any(), eq(t.getId())))
                .thenReturn(List.of(solapada));

        assertThatThrownBy(() -> tutoriaService.cambiarEstado(t.getId(), EstadoTutoria.CONFIRMADA))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("solapa");

        verify(tutoriaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_aCancelada_noRevalida() {
        autenticarComo(estudiante);
        Tutoria t = tutoriaConEstado(EstadoTutoria.PENDIENTE);

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaRepository.save(t)).thenReturn(t);
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaMapper.toResponse(t)).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.cambiarEstado(t.getId(), EstadoTutoria.CANCELADA);

        assertThat(t.getEstado()).isEqualTo(EstadoTutoria.CANCELADA);
        verify(disponibilidadTutoriaRepository, never()).findByDocenteIdAndDiaSemana(any(), any());
        verify(tutoriaRepository, never()).findConfirmadasCandidatasSolape(any(), any(), any(), any(), any());
    }

    @Test
    void cambiarEstado_confirmadaACompletada_comoDocente_ok() {
        autenticarComo(docente);
        Tutoria t = tutoriaConEstado(EstadoTutoria.CONFIRMADA);

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaRepository.save(t)).thenReturn(t);
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaMapper.toResponse(t)).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.cambiarEstado(t.getId(), EstadoTutoria.COMPLETADA);

        assertThat(t.getEstado()).isEqualTo(EstadoTutoria.COMPLETADA);
    }

    @Test
    void cambiarEstado_confirmadaACompletada_comoEstudiante_lanzaAccessDenied() {
        autenticarComo(estudiante);
        Tutoria t = tutoriaConEstado(EstadoTutoria.CONFIRMADA);

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));

        assertThatThrownBy(() -> tutoriaService.cambiarEstado(t.getId(), EstadoTutoria.COMPLETADA))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("docente");
    }

    @Test
    void cambiarEstado_comoTercero_lanzaAccessDenied() {
        Usuario tercero = new Usuario();
        tercero.setId(UUID.randomUUID());
        tercero.setRoles(Set.of());
        autenticarComo(tercero);

        Tutoria t = tutoriaConEstado(EstadoTutoria.PENDIENTE);

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));

        assertThatThrownBy(() -> tutoriaService.cambiarEstado(t.getId(), EstadoTutoria.CONFIRMADA))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cambiarEstado_mismoEstado_lanzaBusinessException() {
        autenticarComo(estudiante);
        Tutoria t = tutoriaConEstado(EstadoTutoria.PENDIENTE);

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));

        assertThatThrownBy(() -> tutoriaService.cambiarEstado(t.getId(), EstadoTutoria.PENDIENTE))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCodigo()).isEqualTo("ESTADO_SIN_CAMBIOS");
                    assertThat(ex.getMessage()).contains("PENDIENTE");
                });
    }

    @Test
    void cambiarEstado_completadaAConfirmada_lanzaBusinessException() {
        autenticarComo(docente);
        Tutoria t = tutoriaConEstado(EstadoTutoria.COMPLETADA);

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));

        assertThatThrownBy(() -> tutoriaService.cambiarEstado(t.getId(), EstadoTutoria.CONFIRMADA))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCodigo()).isEqualTo("TRANSICION_ESTADO_INVALIDA");
                    assertThat(ex.getMessage()).contains("COMPLETADA").contains("CONFIRMADA");
                });
    }

    @Test
    void cambiarEstado_canceladaAConfirmada_lanzaBusinessException() {
        autenticarComo(docente);
        Tutoria t = tutoriaConEstado(EstadoTutoria.CANCELADA);

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));

        assertThatThrownBy(() -> tutoriaService.cambiarEstado(t.getId(), EstadoTutoria.CONFIRMADA))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCodigo()).isEqualTo("TRANSICION_ESTADO_INVALIDA");
                });
    }

    @Test
    void cambiarEstado_pendienteACancelada_comoEstudiante_ok() {
        autenticarComo(estudiante);
        Tutoria t = tutoriaConEstado(EstadoTutoria.PENDIENTE);

        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaRepository.save(t)).thenReturn(t);
        when(tutoriaRepository.findWithEstudianteDocenteCursoById(t.getId()))
                .thenReturn(Optional.of(t));
        when(tutoriaMapper.toResponse(t)).thenReturn(mock(TutoriaResponse.class));

        tutoriaService.cambiarEstado(t.getId(), EstadoTutoria.CANCELADA);

        assertThat(t.getEstado()).isEqualTo(EstadoTutoria.CANCELADA);
    }
}
