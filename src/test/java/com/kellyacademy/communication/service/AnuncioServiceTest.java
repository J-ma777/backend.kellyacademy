package com.kellyacademy.communication.service;

import com.kellyacademy.communication.dto.request.ActualizarAnuncioRequest;
import com.kellyacademy.communication.dto.request.CrearAnuncioRequest;
import com.kellyacademy.communication.dto.response.AnuncioResponse;
import com.kellyacademy.communication.entity.Anuncio;
import com.kellyacademy.communication.mapper.AnuncioMapper;
import com.kellyacademy.communication.repository.AnuncioRepository;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.security.user.CustomUserDetails;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnuncioServiceTest {

    @Mock private AnuncioRepository anuncioRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private AnuncioMapper anuncioMapper;
    @Mock private NotificacionService notificacionService;

    @InjectMocks private AnuncioService anuncioService;

    private UUID cursoId;
    private UUID docenteDuenoId;
    private UUID anuncioId;
    private Curso curso;
    private Usuario docente;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        cursoId = UUID.randomUUID();
        docenteDuenoId = UUID.randomUUID();
        anuncioId = UUID.randomUUID();

        Rol rolDocente = new Rol();
        rolDocente.setNombre("DOCENTE");

        docente = new Usuario();
        docente.setId(docenteDuenoId);
        docente.setNombre("Docente");
        docente.setApellido("Dueno");
        docente.setCorreoElectronico("docente@kelly.com");
        docente.setRoles(Set.of(rolDocente));

        curso = new Curso();
        curso.setId(cursoId);
        curso.setDocente(docente);
        curso.setTitulo("Curso Test");

        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ADMINISTRADOR");
        admin = new Usuario();
        admin.setId(UUID.randomUUID());
        admin.setRoles(Set.of(rolAdmin));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Usuario u) {
        CustomUserDetails details = new CustomUserDetails(u);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // -------- crear --------

    @Test
    void crear_cuandoTodoValido_retornaResponseYNotifica() {
        autenticarComo(docente);

        CrearAnuncioRequest request = new CrearAnuncioRequest(cursoId, "Titulo", "Cuerpo");
        Anuncio guardada = new Anuncio();
        guardada.setId(anuncioId);
        guardada.setCurso(curso);
        guardada.setAutor(docente);
        guardada.setTitulo("Titulo");
        guardada.setActivo(true);

        Usuario estudiante = new Usuario();
        estudiante.setId(UUID.randomUUID());

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);

        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(usuarioRepository.findById(docenteDuenoId)).thenReturn(Optional.of(docente));
        when(anuncioMapper.toEntity(request)).thenReturn(new Anuncio());
        when(anuncioRepository.save(any(Anuncio.class))).thenReturn(guardada);
        when(matriculaRepository.findWithEstudianteByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA))
                .thenReturn(List.of(matricula));
        when(anuncioMapper.toResponse(guardada)).thenReturn(mock(AnuncioResponse.class));

        AnuncioResponse response = anuncioService.crear(request);

        assertThat(response).isNotNull();
        verify(notificacionService).crear(eq(estudiante.getId()), any(), anyString(), eq(null), anyString());
    }

    @Test
    void crear_cuandoCursoNoExiste_lanzaResourceNotFound() {
        autenticarComo(docente);

        CrearAnuncioRequest request = new CrearAnuncioRequest(cursoId, "Titulo", "Cuerpo");
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anuncioService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_cuandoDocenteAjeno_lanzaAccessDenied() {
        Rol rolDocente = new Rol();
        rolDocente.setNombre("DOCENTE");
        Usuario otroDocente = new Usuario();
        otroDocente.setId(UUID.randomUUID());
        otroDocente.setRoles(Set.of(rolDocente));
        autenticarComo(otroDocente);

        CrearAnuncioRequest request = new CrearAnuncioRequest(cursoId, "Titulo", "Cuerpo");
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));

        assertThatThrownBy(() -> anuncioService.crear(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void crear_cuandoNoHayMatriculados_noNotifica() {
        autenticarComo(docente);

        CrearAnuncioRequest request = new CrearAnuncioRequest(cursoId, "Titulo", "Cuerpo");
        Anuncio guardada = new Anuncio();
        guardada.setId(anuncioId);
        guardada.setCurso(curso);
        guardada.setAutor(docente);

        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(usuarioRepository.findById(docenteDuenoId)).thenReturn(Optional.of(docente));
        when(anuncioMapper.toEntity(request)).thenReturn(new Anuncio());
        when(anuncioRepository.save(any(Anuncio.class))).thenReturn(guardada);
        when(matriculaRepository.findWithEstudianteByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA))
                .thenReturn(List.of());
        when(anuncioMapper.toResponse(guardada)).thenReturn(mock(AnuncioResponse.class));

        anuncioService.crear(request);

        verify(notificacionService, never()).crear(any(), any(), anyString(), any(), any());
    }

    // -------- actualizar --------

    @Test
    void actualizar_cuandoEsDocenteDueno_actualiza() {
        autenticarComo(docente);
        Anuncio anuncio = new Anuncio();
        anuncio.setId(anuncioId);
        anuncio.setCurso(curso);

        ActualizarAnuncioRequest request = new ActualizarAnuncioRequest("Nuevo", "Cuerpo nuevo");

        when(anuncioRepository.findWithCursoAndAutorById(anuncioId)).thenReturn(Optional.of(anuncio));
        when(anuncioMapper.toResponse(anuncio)).thenReturn(mock(AnuncioResponse.class));

        anuncioService.actualizar(anuncioId, request);

        verify(anuncioMapper).actualizarDesdeRequest(request, anuncio);
    }

    @Test
    void actualizar_cuandoEsDocenteAjeno_lanzaAccessDenied() {
        Rol rolDocente = new Rol();
        rolDocente.setNombre("DOCENTE");
        Usuario otroDocente = new Usuario();
        otroDocente.setId(UUID.randomUUID());
        otroDocente.setRoles(Set.of(rolDocente));
        autenticarComo(otroDocente);

        Anuncio anuncio = new Anuncio();
        anuncio.setId(anuncioId);
        anuncio.setCurso(curso);

        when(anuncioRepository.findWithCursoAndAutorById(anuncioId)).thenReturn(Optional.of(anuncio));

        assertThatThrownBy(() -> anuncioService.actualizar(anuncioId,
                new ActualizarAnuncioRequest("Nuevo", "Cuerpo")))
                .isInstanceOf(AccessDeniedException.class);
    }

    // -------- archivar --------

    @Test
    void archivar_cuandoEsDocenteDueno_marcaInactivo() {
        autenticarComo(docente);
        Anuncio anuncio = new Anuncio();
        anuncio.setId(anuncioId);
        anuncio.setCurso(curso);
        anuncio.setActivo(true);

        when(anuncioRepository.findWithCursoAndAutorById(anuncioId)).thenReturn(Optional.of(anuncio));
        when(anuncioMapper.toResponse(anuncio)).thenReturn(mock(AnuncioResponse.class));

        anuncioService.archivar(anuncioId);

        assertThat(anuncio.getActivo()).isFalse();
    }

    @Test
    void archivar_cuandoYaEstabaArchivado_esIdempotente() {
        autenticarComo(docente);
        Anuncio anuncio = new Anuncio();
        anuncio.setId(anuncioId);
        anuncio.setCurso(curso);
        anuncio.setActivo(false);

        when(anuncioRepository.findWithCursoAndAutorById(anuncioId)).thenReturn(Optional.of(anuncio));
        when(anuncioMapper.toResponse(anuncio)).thenReturn(mock(AnuncioResponse.class));

        anuncioService.archivar(anuncioId);

        assertThat(anuncio.getActivo()).isFalse();
    }

    // -------- eliminar --------

    @Test
    void eliminar_cuandoEsAdmin_borra() {
        autenticarComo(admin);
        Anuncio anuncio = new Anuncio();
        anuncio.setId(anuncioId);
        anuncio.setCurso(curso);

        when(anuncioRepository.findWithCursoAndAutorById(anuncioId)).thenReturn(Optional.of(anuncio));

        anuncioService.eliminar(anuncioId);

        verify(anuncioRepository).delete(anuncio);
    }

    @Test
    void eliminar_cuandoEsDocenteAjeno_lanzaAccessDenied() {
        Rol rolDocente = new Rol();
        rolDocente.setNombre("DOCENTE");
        Usuario otroDocente = new Usuario();
        otroDocente.setId(UUID.randomUUID());
        otroDocente.setRoles(Set.of(rolDocente));
        autenticarComo(otroDocente);

        Anuncio anuncio = new Anuncio();
        anuncio.setId(anuncioId);
        anuncio.setCurso(curso);

        when(anuncioRepository.findWithCursoAndAutorById(anuncioId)).thenReturn(Optional.of(anuncio));

        assertThatThrownBy(() -> anuncioService.eliminar(anuncioId))
                .isInstanceOf(AccessDeniedException.class);
    }
}