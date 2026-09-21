package com.kellyacademy.calendar.service;

import com.kellyacademy.calendar.dto.request.ActualizarEventoRequest;
import com.kellyacademy.calendar.dto.request.CrearEventoRequest;
import com.kellyacademy.calendar.dto.response.EventoResponse;
import com.kellyacademy.calendar.entity.Evento;
import com.kellyacademy.calendar.enums.TipoEvento;
import com.kellyacademy.calendar.mapper.EventoMapper;
import com.kellyacademy.calendar.repository.EventoRepository;
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock EventoRepository eventoRepository;
    @Mock EventoMapper eventoMapper;
    @Mock UsuarioRepository usuarioRepository;
    @Mock CursoRepository cursoRepository;
    @Mock MatriculaRepository matriculaRepository;

    @InjectMocks EventoService eventoService;

    private UUID usuarioId;
    private Usuario usuario;
    private Evento evento;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNombre("Ana");
        usuario.setApellido("Torres");

        evento = new Evento();
        evento.setId(UUID.randomUUID());
        evento.setUsuario(usuario);
        evento.setTitulo("Examen");
        evento.setTipo(TipoEvento.EXAMEN);
        evento.setInicio(LocalDateTime.now().plusDays(1));
        evento.setFin(LocalDateTime.now().plusDays(1).plusHours(2));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Usuario u, String rolNombre) {
        // Construye el grafo minimo Rol + Permiso para que CustomUserDetails.getAuthorities()
        // produzca "ROLE_<rol>" y los permisos del rol.
        Rol rol = new Rol();
        rol.setNombre(rolNombre);
        rol.setPermisos(new HashSet<>());
        u.setRoles(Set.of(rol));

        CustomUserDetails details = new CustomUserDetails(u);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        details,
                        null,
                        details.getAuthorities()
                )
        );
    }

    // ------------------------------------------------------------------------
    // CREAR
    // ------------------------------------------------------------------------

    @Test
    void crear_sinCurso_ok() {
        autenticarComo(usuario, "ESTUDIANTE");

        CrearEventoRequest req = new CrearEventoRequest(
                null, "Estudiar", "Repaso", TipoEvento.OTRO,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                "Sala 1"
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(eventoMapper.toEntity(req)).thenReturn(evento);
        when(eventoRepository.save(evento)).thenReturn(evento);
        when(eventoRepository.findWithUsuarioAndCursoById(evento.getId())).thenReturn(Optional.of(evento));
        when(eventoMapper.toResponse(evento)).thenReturn(mock(EventoResponse.class));

        eventoService.crear(req);

        verify(eventoRepository).save(evento);
        assertThat(evento.getUsuario()).isEqualTo(usuario);
        assertThat(evento.getCurso()).isNull();
    }

    @Test
    void crear_finAntesDeInicio_lanzaBusinessException() {
        autenticarComo(usuario, "ESTUDIANTE");

        CrearEventoRequest req = new CrearEventoRequest(
                null, "X", null, TipoEvento.OTRO,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1),
                null
        );

        assertThatThrownBy(() -> eventoService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("posterior");

        verifyNoInteractions(eventoRepository);
    }

    @Test
    void crear_conCurso_usuarioNoPertenece_lanzaBusinessException() {
        autenticarComo(usuario, "ESTUDIANTE");

        UUID cursoId = UUID.randomUUID();
        Curso curso = new Curso();
        curso.setId(cursoId);

        CrearEventoRequest req = new CrearEventoRequest(
                cursoId, "X", null, TipoEvento.OTRO,
                LocalDateTime.now().plusDays(1),
                null,
                null
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(matriculaRepository.findByCursoIdAndEstudianteId(cursoId, usuarioId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventoService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("matricula ACTIVA");
    }

    @Test
    void crear_conCurso_docenteDueno_ok() {
        autenticarComo(usuario, "DOCENTE");

        UUID cursoId = UUID.randomUUID();
        Curso curso = new Curso();
        curso.setId(cursoId);
        curso.setDocente(usuario);

        CrearEventoRequest req = new CrearEventoRequest(
                cursoId, "X", null, TipoEvento.EXAMEN,
                LocalDateTime.now().plusDays(1),
                null,
                null
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(eventoMapper.toEntity(req)).thenReturn(evento);
        when(eventoRepository.save(evento)).thenReturn(evento);
        when(eventoRepository.findWithUsuarioAndCursoById(evento.getId())).thenReturn(Optional.of(evento));
        when(eventoMapper.toResponse(evento)).thenReturn(mock(EventoResponse.class));

        eventoService.crear(req);

        verify(eventoRepository).save(evento);
    }

    @Test
    void crear_conCurso_estudianteMatriculadoActivo_ok() {
        autenticarComo(usuario, "ESTUDIANTE");

        UUID cursoId = UUID.randomUUID();
        Curso curso = new Curso();
        curso.setId(cursoId);

        Matricula m = new Matricula();
        m.setEstado(EstadoMatricula.ACTIVA);

        CrearEventoRequest req = new CrearEventoRequest(
                cursoId, "X", null, TipoEvento.CLASE,
                LocalDateTime.now().plusDays(1),
                null,
                null
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(matriculaRepository.findByCursoIdAndEstudianteId(cursoId, usuarioId))
                .thenReturn(Optional.of(m));
        when(eventoMapper.toEntity(req)).thenReturn(evento);
        when(eventoRepository.save(evento)).thenReturn(evento);
        when(eventoRepository.findWithUsuarioAndCursoById(evento.getId())).thenReturn(Optional.of(evento));
        when(eventoMapper.toResponse(evento)).thenReturn(mock(EventoResponse.class));

        eventoService.crear(req);

        verify(eventoRepository).save(evento);
    }

    @Test
    void crear_conCurso_estudianteMatriculadoNoActivo_lanzaBusinessException() {
        autenticarComo(usuario, "ESTUDIANTE");

        UUID cursoId = UUID.randomUUID();
        Curso curso = new Curso();
        curso.setId(cursoId);

        Matricula m = new Matricula();
        m.setEstado(EstadoMatricula.ABANDONADA);

        CrearEventoRequest req = new CrearEventoRequest(
                cursoId, "X", null, TipoEvento.OTRO,
                LocalDateTime.now().plusDays(1),
                null,
                null
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(matriculaRepository.findByCursoIdAndEstudianteId(cursoId, usuarioId))
                .thenReturn(Optional.of(m));

        assertThatThrownBy(() -> eventoService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("matricula ACTIVA");
    }

    // ------------------------------------------------------------------------
    // OBTENER / ACTUALIZAR / ELIMINAR
    // ------------------------------------------------------------------------

    @Test
    void obtener_propio_ok() {
        autenticarComo(usuario, "ESTUDIANTE");

        when(eventoRepository.findWithUsuarioAndCursoById(evento.getId()))
                .thenReturn(Optional.of(evento));
        when(eventoMapper.toResponse(evento)).thenReturn(mock(EventoResponse.class));

        eventoService.obtener(evento.getId());

        verify(eventoMapper).toResponse(evento);
    }

    @Test
    void obtener_ajeno_lanzaAccessDenied() {
        Usuario otro = new Usuario();
        otro.setId(UUID.randomUUID());
        autenticarComo(otro, "ESTUDIANTE");

        when(eventoRepository.findWithUsuarioAndCursoById(evento.getId()))
                .thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> eventoService.obtener(evento.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void obtener_admin_ok() {
        Usuario admin = new Usuario();
        admin.setId(UUID.randomUUID());
        autenticarComo(admin, "ADMINISTRADOR");

        when(eventoRepository.findWithUsuarioAndCursoById(evento.getId()))
                .thenReturn(Optional.of(evento));
        when(eventoMapper.toResponse(evento)).thenReturn(mock(EventoResponse.class));

        eventoService.obtener(evento.getId());

        verify(eventoMapper).toResponse(evento);
    }

    @Test
    void obtener_inexistente_lanzaResourceNotFound() {
        autenticarComo(usuario, "ESTUDIANTE");
        UUID id = UUID.randomUUID();

        when(eventoRepository.findWithUsuarioAndCursoById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventoService.obtener(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizar_propio_ok() {
        autenticarComo(usuario, "ESTUDIANTE");

        ActualizarEventoRequest req = new ActualizarEventoRequest(
                "Nuevo", null, TipoEvento.OTRO,
                LocalDateTime.now().plusDays(3),
                null,
                null
        );

        when(eventoRepository.findWithUsuarioAndCursoById(evento.getId()))
                .thenReturn(Optional.of(evento));
        when(eventoRepository.save(evento)).thenReturn(evento);
        when(eventoMapper.toResponse(evento)).thenReturn(mock(EventoResponse.class));

        eventoService.actualizar(evento.getId(), req);

        verify(eventoMapper).actualizarDesdeRequest(req, evento);
        verify(eventoRepository).save(evento);
    }

    @Test
    void actualizar_finInvalido_lanzaBusinessException() {
        autenticarComo(usuario, "ESTUDIANTE");

        ActualizarEventoRequest req = new ActualizarEventoRequest(
                "X", null, TipoEvento.OTRO,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1),
                null
        );

        when(eventoRepository.findWithUsuarioAndCursoById(evento.getId()))
                .thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> eventoService.actualizar(evento.getId(), req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    void actualizar_ajeno_lanzaAccessDenied() {
        Usuario otro = new Usuario();
        otro.setId(UUID.randomUUID());
        autenticarComo(otro, "ESTUDIANTE");

        ActualizarEventoRequest req = new ActualizarEventoRequest(
                "X", null, TipoEvento.OTRO,
                LocalDateTime.now().plusDays(3),
                null,
                null
        );

        when(eventoRepository.findWithUsuarioAndCursoById(evento.getId()))
                .thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> eventoService.actualizar(evento.getId(), req))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void eliminar_propio_ok() {
        autenticarComo(usuario, "ESTUDIANTE");

        when(eventoRepository.findWithUsuarioAndCursoById(evento.getId()))
                .thenReturn(Optional.of(evento));

        eventoService.eliminar(evento.getId());

        verify(eventoRepository).delete(evento);
    }

    @Test
    void eliminar_ajeno_lanzaAccessDenied() {
        Usuario otro = new Usuario();
        otro.setId(UUID.randomUUID());
        autenticarComo(otro, "ESTUDIANTE");

        when(eventoRepository.findWithUsuarioAndCursoById(evento.getId()))
                .thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> eventoService.eliminar(evento.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }
}