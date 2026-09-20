package com.kellyacademy.user.service;

import com.kellyacademy.security.user.CustomUserDetails;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.CorreoYaRegistradoException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.user.dto.request.CrearUsuarioRequest;
import com.kellyacademy.user.dto.response.UsuarioResponse;
import com.kellyacademy.user.entity.Rol;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.enums.EstadoUsuario;
import com.kellyacademy.user.mapper.UsuarioMapper;
import com.kellyacademy.user.repository.RolRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private UUID usuarioAutenticadoId;

    @BeforeEach
    void setUp() {
        usuarioAutenticadoId = UUID.randomUUID();
        // Por defecto, el usuario autenticado es un ESTUDIANTE (no admin).
        autenticarComo(usuarioAutenticadoId, "ESTUDIANTE");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // HELPERS

    /*
     Construye el SecurityContext con un CustomUserDetails cuyo Usuario tiene
     el rol indicado. Es necesario que el Usuario tenga el Rol asignado para
     que CustomUserDetails.getAuthorities() genere las authorities reales
     (SecurityUtils.esAdmin() lee de ahi).
     */
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
                new UsernamePasswordAuthenticationToken(
                        details,
                        null,
                        details.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private CrearUsuarioRequest requestValido() {
        return new CrearUsuarioRequest(
                "Juan",
                "Perez",
                "juan@kelly.com",
                "Password123",
                null,
                Set.of("ESTUDIANTE")
        );
    }

    // TESTS

    @Test
    void crear_correoDuplicado_lanzaCorreoYaRegistradoException() {

        CrearUsuarioRequest request = requestValido();
        when(usuarioRepository.existsByCorreoElectronico(request.correoElectronico()))
                .thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(CorreoYaRegistradoException.class)
                .hasMessageContaining(request.correoElectronico());

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_exitoso_estadoActivoYPasswordHasheada() {

        CrearUsuarioRequest request = requestValido();

        Rol rolEstudiante = new Rol();
        rolEstudiante.setId(UUID.randomUUID());
        rolEstudiante.setNombre("ESTUDIANTE");

        Usuario usuarioMapeado = new Usuario();
        usuarioMapeado.setNombre(request.nombre());
        usuarioMapeado.setApellido(request.apellido());
        usuarioMapeado.setCorreoElectronico(request.correoElectronico());

        Usuario usuarioGuardado = new Usuario();
        UUID idGenerado = UUID.randomUUID();
        usuarioGuardado.setId(idGenerado);
        usuarioGuardado.setNombre(request.nombre());
        usuarioGuardado.setApellido(request.apellido());
        usuarioGuardado.setCorreoElectronico(request.correoElectronico());
        usuarioGuardado.setContrasena("$2a$12$hashed");
        usuarioGuardado.setEstado(EstadoUsuario.ACTIVO);
        usuarioGuardado.setRoles(Set.of(rolEstudiante));

        UsuarioResponse responseEsperado = new UsuarioResponse(
                idGenerado,
                request.nombre(),
                request.apellido(),
                request.correoElectronico(),
                null,
                EstadoUsuario.ACTIVO,
                List.of("ESTUDIANTE"),
                null,
                null
        );

        when(usuarioRepository.existsByCorreoElectronico(request.correoElectronico()))
                .thenReturn(false);
        when(rolRepository.findByNombre("ESTUDIANTE"))
                .thenReturn(Optional.of(rolEstudiante));
        when(usuarioMapper.toEntity(request)).thenReturn(usuarioMapeado);
        when(passwordEncoder.encode(request.contrasena())).thenReturn("$2a$12$hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(usuarioRepository.findWithRolesById(idGenerado))
                .thenReturn(Optional.of(usuarioGuardado));
        when(usuarioMapper.toResponse(usuarioGuardado)).thenReturn(responseEsperado);

        UsuarioResponse resultado = usuarioService.crear(request);

        assertThat(resultado.estado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(resultado.roles()).containsExactly("ESTUDIANTE");

        verify(passwordEncoder).encode(request.contrasena());
        verify(usuarioRepository).save(org.mockito.ArgumentMatchers.argThat(u ->
                "$2a$12$hashed".equals(u.getContrasena())
                        && u.getEstado() == EstadoUsuario.ACTIVO
                        && u.getRoles().contains(rolEstudiante)
        ));
    }

    @Test
    void crear_rolInexistente_lanzaBusinessException() {

        CrearUsuarioRequest request = requestValido();

        when(usuarioRepository.existsByCorreoElectronico(request.correoElectronico()))
                .thenReturn(false);
        when(rolRepository.findByNombre("ESTUDIANTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCodigo()).isEqualTo("ROL_INEXISTENTE");
                    assertThat(ex.getMessage()).contains("ESTUDIANTE");
                });

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void obtener_idInexistente_lanzaResourceNotFoundException() {

        // Autenticamos como admin para que la autorizacion fina no bloquee antes del findById.
        autenticarComo(usuarioAutenticadoId, "ADMINISTRADOR");

        UUID idBuscado = UUID.randomUUID();
        when(usuarioRepository.findWithRolesById(idBuscado)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtener(idBuscado))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(idBuscado.toString());
    }

    @Test
    void obtener_otroUsuarioSinSerAdmin_lanzaAccessDeniedException() {

        // El usuario autenticado es ESTUDIANTE (setUp), intenta leer a otro.
        UUID otroId = UUID.randomUUID();

        assertThatThrownBy(() -> usuarioService.obtener(otroId))
                .isInstanceOf(AccessDeniedException.class);

        verify(usuarioRepository, never()).findWithRolesById(any());
    }

    @Test
    void eliminar_aSiMismo_lanzaBusinessException() {

        autenticarComo(usuarioAutenticadoId, "ADMINISTRADOR");

        assertThatThrownBy(() -> usuarioService.eliminar(usuarioAutenticadoId))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCodigo()).isEqualTo("NO_PUEDE_AUTODELETARSE");
                    assertThat(ex.getMessage()).contains("no puede eliminar su propia cuenta");
                });

        verify(usuarioRepository, never()).delete(any());
    }
}