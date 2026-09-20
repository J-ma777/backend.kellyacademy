package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.mapper.CursoMapper;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.security.user.CustomUserDetails;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.user.dto.response.UsuarioResumenResponse;
import com.kellyacademy.user.entity.Rol;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.enums.EstadoUsuario;
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

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CursoMapper cursoMapper;

    @InjectMocks
    private CursoService cursoService;

    private UUID adminId;
    private UUID docenteId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        docenteId = UUID.randomUUID();
        autenticarComo(adminId, "ADMINISTRADOR");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(UUID id, String nombreRol) {
        Rol rol = new Rol();
        rol.setId(UUID.randomUUID());
        rol.setNombre(nombreRol);

        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setCorreoElectronico("auth@kelly.com");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setRoles(Set.of(rol));

        CustomUserDetails details = new CustomUserDetails(usuario);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Usuario docenteConRol() {
        Rol rolDocente = new Rol();
        rolDocente.setId(UUID.randomUUID());
        rolDocente.setNombre("DOCENTE");

        Usuario docente = new Usuario();
        docente.setId(docenteId);
        docente.setNombre("Docente");
        docente.setApellido("Test");
        docente.setCorreoElectronico("docente@kelly.com");
        docente.setEstado(EstadoUsuario.ACTIVO);
        docente.setRoles(Set.of(rolDocente));
        return docente;
    }

    private Usuario docenteSinRol() {
        Rol rolEstudiante = new Rol();
        rolEstudiante.setId(UUID.randomUUID());
        rolEstudiante.setNombre("ESTUDIANTE");

        Usuario usuario = new Usuario();
        usuario.setId(docenteId);
        usuario.setNombre("NoDocente");
        usuario.setApellido("Test");
        usuario.setCorreoElectronico("nodocente@kelly.com");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setRoles(Set.of(rolEstudiante));
        return usuario;
    }

    private CrearCursoRequest requestValido() {
        return new CrearCursoRequest(
                docenteId,
                "Curso de Ingles",
                "Descripcion",
                NivelCefr.B1,
                "Lunes y Miercoles 19:00",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusMonths(3),
                30
        );
    }

    @Test
    void crear_docenteSinRolDocente_lanzaBusinessException() {

        CrearCursoRequest request = requestValido();
        when(usuarioRepository.findWithRolesById(docenteId))
                .thenReturn(Optional.of(docenteSinRol()));

        assertThatThrownBy(() -> cursoService.crear(request))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCodigo()).isEqualTo("DOCENTE_SIN_ROL");
                    assertThat(ex.getMessage()).contains("DOCENTE");
                });

        verify(cursoRepository, never()).save(any());
    }

    @Test
    void crear_docenteInexistente_lanzaResourceNotFoundException() {

        CrearCursoRequest request = requestValido();
        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cursoService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(docenteId.toString());

        verify(cursoRepository, never()).save(any());
    }

    @Test
    void crear_fechasInvalidas_lanzaBusinessException() {

        CrearCursoRequest request = new CrearCursoRequest(
                docenteId,
                "Curso",
                "Desc",
                NivelCefr.A1,
                null,
                LocalDate.now().plusMonths(3),  // inicio despues del fin
                LocalDate.now().plusDays(1),    // fin antes del inicio
                30
        );

        when(usuarioRepository.findWithRolesById(docenteId))
                .thenReturn(Optional.of(docenteConRol()));

        assertThatThrownBy(() -> cursoService.crear(request))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCodigo()).isEqualTo("FECHAS_INVALIDAS");
                });

        verify(cursoRepository, never()).save(any());
    }

    @Test
    void crear_exitoso_estadoBorrador() {

        CrearCursoRequest request = requestValido();
        Usuario docente = docenteConRol();

        Curso cursoMapeado = new Curso();
        cursoMapeado.setTitulo(request.titulo());
        cursoMapeado.setNivelCefr(request.nivelCefr());

        Curso cursoGuardado = new Curso();
        UUID idGenerado = UUID.randomUUID();
        cursoGuardado.setId(idGenerado);
        cursoGuardado.setTitulo(request.titulo());
        cursoGuardado.setDocente(docente);
        cursoGuardado.setEstado(EstadoCurso.BORRADOR);
        cursoGuardado.setNivelCefr(request.nivelCefr());
        cursoGuardado.setCapacidadMaxima(request.capacidadMaxima());

        UsuarioResumenResponse docenteResumen = new UsuarioResumenResponse(
                docenteId, "Docente", "Test", "docente@kelly.com", null, EstadoUsuario.ACTIVO
        );
        CursoResponse responseEsperado = new CursoResponse(
                idGenerado, docenteResumen, request.titulo(), "Descripcion",
                NivelCefr.B1, "Lunes y Miercoles 19:00",
                request.fechaInicio(), request.fechaFin(), 30,
                EstadoCurso.BORRADOR, null, null
        );

        when(usuarioRepository.findWithRolesById(docenteId)).thenReturn(Optional.of(docente));
        when(cursoMapper.toEntity(request)).thenReturn(cursoMapeado);
        when(cursoRepository.save(any(Curso.class))).thenReturn(cursoGuardado);
        when(cursoRepository.findWithDocenteById(idGenerado)).thenReturn(Optional.of(cursoGuardado));
        when(cursoMapper.toResponse(cursoGuardado)).thenReturn(responseEsperado);

        CursoResponse resultado = cursoService.crear(request);

        assertThat(resultado.estado()).isEqualTo(EstadoCurso.BORRADOR);
        assertThat(resultado.titulo()).isEqualTo(request.titulo());

        // Verificacion de argumento pasado a save() via ArgumentCaptor en lugar de argThat.
        // Mejora el debugging: si falla, el assert dice cual propiedad fallo, no un error generico.
        ArgumentCaptor<Curso> captor = ArgumentCaptor.forClass(Curso.class);
        verify(cursoRepository).save(captor.capture());

        Curso cursoPersistido = captor.getValue();
        assertThat(cursoPersistido.getEstado()).isEqualTo(EstadoCurso.BORRADOR);
        assertThat(cursoPersistido.getDocente()).isSameAs(docente);
    }

    @Test
    void obtener_noEncontrado_lanzaResourceNotFoundException() {

        UUID id = UUID.randomUUID();
        when(cursoRepository.findWithDocenteById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cursoService.obtener(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void eliminar_docenteNoPropietarioSinSerAdmin_lanzaAccessDeniedException() {

        UUID otroDocenteId = UUID.randomUUID();
        autenticarComo(otroDocenteId, "DOCENTE");

        Curso curso = new Curso();
        curso.setId(UUID.randomUUID());
        curso.setDocente(docenteConRol());  // docente original, no el autenticado

        UUID cursoId = curso.getId();

        when(cursoRepository.findWithDocenteById(cursoId))
                .thenReturn(Optional.of(curso));

        assertThatThrownBy(() -> cursoService.eliminar(cursoId))
                .isInstanceOf(AccessDeniedException.class);

        verify(cursoRepository, never()).delete(any(Curso.class));
    }

    @Test
    void eliminar_adminPuedeEliminarCualquierCurso() {

        Curso curso = new Curso();
        UUID cursoId = UUID.randomUUID();
        curso.setId(cursoId);
        curso.setDocente(docenteConRol());

        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));

        cursoService.eliminar(cursoId);

        verify(cursoRepository).delete(curso);
    }
}