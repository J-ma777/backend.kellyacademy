package com.kellyacademy.communication.service;

import com.kellyacademy.communication.dto.response.NotificacionResponse;
import com.kellyacademy.communication.entity.Notificacion;
import com.kellyacademy.communication.enums.TipoNotificacion;
import com.kellyacademy.communication.mapper.NotificacionMapper;
import com.kellyacademy.communication.repository.NotificacionRepository;
import com.kellyacademy.communication.service.NotificacionService.NuevaNotificacion;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock private NotificacionRepository notificacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NotificacionMapper notificacionMapper;

    @InjectMocks private NotificacionService notificacionService;

    private UUID usuarioId;
    private UUID otroUsuarioId;
    private UUID notificacionId;
    private Usuario usuario;
    private Usuario otroUsuario;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        otroUsuarioId = UUID.randomUUID();
        notificacionId = UUID.randomUUID();

        usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNombre("Ana");
        usuario.setApellido("Torres");
        usuario.setCorreoElectronico("ana@kelly.com");
        usuario.setRoles(Set.of(rolEstudiante()));

        otroUsuario = new Usuario();
        otroUsuario.setId(otroUsuarioId);
        otroUsuario.setRoles(Set.of(rolEstudiante()));

        admin = new Usuario();
        admin.setId(UUID.randomUUID());
        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ADMINISTRADOR");
        admin.setRoles(Set.of(rolAdmin));
    }

    private Rol rolEstudiante() {
        Rol r = new Rol();
        r.setNombre("ESTUDIANTE");
        return r;
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

    private Notificacion notificacionDe(Usuario dueno, boolean leida) {
        Notificacion n = new Notificacion();
        n.setId(notificacionId);
        n.setUsuario(dueno);
        n.setTipo(TipoNotificacion.SISTEMA);
        n.setTitulo("Test");
        n.setLeida(leida);
        return n;
    }

    private void stubUsuariosYSaveAll(List<Usuario> usuarios) {
        when(usuarioRepository.findAllById(any())).thenReturn(usuarios);
        when(notificacionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificacionMapper.toResponse(any(Notificacion.class)))
                .thenReturn(mock(NotificacionResponse.class));
    }

    // -------- crear (interno) --------

    @Test
    void crear_cuandoTodoValido_retornaResponse() {
        // No requiere autenticacion: crear es interno.
        stubUsuariosYSaveAll(List.of(usuario));

        NotificacionResponse response = notificacionService.crear(
                usuarioId, TipoNotificacion.SISTEMA, "Titulo", "Cuerpo", null
        );

        assertThat(response).isNotNull();
        verify(notificacionRepository).saveAll(argThat(lista -> ((List<?>) lista).size() == 1));
        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void crear_delegaEnCrearBatchConUnElemento() {
        stubUsuariosYSaveAll(List.of(usuario));

        notificacionService.crear(usuarioId, TipoNotificacion.SISTEMA, "Titulo", "Cuerpo", "/api/x");

        verify(usuarioRepository).findAllById(argThat(ids -> {
            List<UUID> lista = new java.util.ArrayList<>();
            ids.forEach(lista::add);
            return lista.size() == 1 && lista.contains(usuarioId);
        }));
        verify(usuarioRepository, never()).findById(any());
        verify(notificacionRepository).saveAll(argThat(lista -> ((List<?>) lista).size() == 1));
        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void crear_cuandoUsuarioNoExiste_lanzaResourceNotFound() {
        when(usuarioRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> notificacionService.crear(
                usuarioId, TipoNotificacion.SISTEMA, "Titulo", "Cuerpo", null
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(usuarioId.toString());
    }

    @Test
    void crearBatch_cuandoListaVacia_noGuarda() {
        List<NotificacionResponse> resultado = notificacionService.crearBatch(List.of());

        assertThat(resultado).isEmpty();
        verify(usuarioRepository, never()).findAllById(any());
        verify(notificacionRepository, never()).saveAll(any());
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void crearBatch_conVariosDestinatarios_guardaTodosEnUnaLlamada() {
        Usuario segundo = new Usuario();
        segundo.setId(otroUsuarioId);

        stubUsuariosYSaveAll(List.of(usuario, segundo));

        List<NuevaNotificacion> lote = List.of(
                new NuevaNotificacion(usuarioId, TipoNotificacion.ANUNCIO, "T1", null, "/api/a"),
                new NuevaNotificacion(otroUsuarioId, TipoNotificacion.ANUNCIO, "T1", null, "/api/a")
        );

        List<NotificacionResponse> resultado = notificacionService.crearBatch(lote);

        assertThat(resultado).hasSize(2);
        verify(usuarioRepository).findAllById(any());
        verify(notificacionRepository, times(1)).saveAll(argThat(lista -> ((List<?>) lista).size() == 2));
        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void crearBatch_validaTituloCuerpoLinkComoCrear() {
        NuevaNotificacion tituloVacio = new NuevaNotificacion(
                usuarioId, TipoNotificacion.SISTEMA, " ", "Cuerpo", null
        );
        NuevaNotificacion cuerpoLargo = new NuevaNotificacion(
                usuarioId, TipoNotificacion.SISTEMA, "Titulo", "x".repeat(501), null
        );
        NuevaNotificacion linkInvalido = new NuevaNotificacion(
                usuarioId, TipoNotificacion.SISTEMA, "Titulo", "Cuerpo", "no-es-url"
        );

        assertThatThrownBy(() -> notificacionService.crearBatch(List.of(tituloVacio)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> notificacionService.crearBatch(List.of(cuerpoLargo)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cuerpo");
        assertThatThrownBy(() -> notificacionService.crearBatch(List.of(linkInvalido)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("URL");

        verify(usuarioRepository, never()).findAllById(any());
        verify(notificacionRepository, never()).saveAll(any());
    }

    @Test
    void crearBatch_cuandoUsuarioNoExiste_lanzaResourceNotFound() {
        when(usuarioRepository.findAllById(any())).thenReturn(List.of());

        NuevaNotificacion item = new NuevaNotificacion(
                usuarioId, TipoNotificacion.SISTEMA, "Titulo", "Cuerpo", null
        );

        assertThatThrownBy(() -> notificacionService.crearBatch(List.of(item)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(usuarioId.toString());
        verify(notificacionRepository, never()).saveAll(any());
    }

    @Test
    void crearBatch_cuandoVariosUsuariosUnoNoExiste_lanzaResourceNotFound() {
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(usuario));

        NuevaNotificacion existente = new NuevaNotificacion(
                usuarioId, TipoNotificacion.SISTEMA, "Titulo", "Cuerpo", null
        );
        NuevaNotificacion faltante = new NuevaNotificacion(
                otroUsuarioId, TipoNotificacion.SISTEMA, "Titulo", "Cuerpo", null
        );

        assertThatThrownBy(() -> notificacionService.crearBatch(List.of(existente, faltante)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(otroUsuarioId.toString());
        verify(notificacionRepository, never()).saveAll(any());
    }

    @Test
    void crear_cuandoLinkInvalido_lanzaBusinessException() {
        assertThatThrownBy(() -> notificacionService.crear(
                usuarioId, TipoNotificacion.SISTEMA, "Titulo", "Cuerpo", "no-es-url"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("URL");
    }

    @Test
    void crear_cuandoTituloVacio_lanzaBusinessException() {
        assertThatThrownBy(() -> notificacionService.crear(
                usuarioId, TipoNotificacion.SISTEMA, " ", "Cuerpo", null
        )).isInstanceOf(BusinessException.class);
    }

    // -------- marcarLeida --------

    @Test
    void marcarLeida_cuandoEsDueno_retornaResponse() {
        autenticarComo(usuario);
        Notificacion n = notificacionDe(usuario, false);
        when(notificacionRepository.findWithUsuarioById(notificacionId)).thenReturn(Optional.of(n));
        when(notificacionMapper.toResponse(n)).thenReturn(mock(NotificacionResponse.class));

        NotificacionResponse response = notificacionService.marcarLeida(notificacionId);

        assertThat(response).isNotNull();
        assertThat(n.getLeida()).isTrue();
    }

    @Test
    void marcarLeida_cuandoYaEstabaLeida_esIdempotente() {
        autenticarComo(usuario);
        Notificacion n = notificacionDe(usuario, true);
        when(notificacionRepository.findWithUsuarioById(notificacionId)).thenReturn(Optional.of(n));
        when(notificacionMapper.toResponse(n)).thenReturn(mock(NotificacionResponse.class));

        NotificacionResponse response = notificacionService.marcarLeida(notificacionId);

        assertThat(response).isNotNull();
        assertThat(n.getLeida()).isTrue();
    }

    @Test
    void marcarLeida_cuandoEsAjeno_lanzaAccessDenied() {
        autenticarComo(otroUsuario);
        Notificacion n = notificacionDe(usuario, false);
        when(notificacionRepository.findWithUsuarioById(notificacionId)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificacionService.marcarLeida(notificacionId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void marcarLeida_cuandoEsAdmin_permite() {
        autenticarComo(admin);
        Notificacion n = notificacionDe(usuario, false);
        when(notificacionRepository.findWithUsuarioById(notificacionId)).thenReturn(Optional.of(n));
        when(notificacionMapper.toResponse(n)).thenReturn(mock(NotificacionResponse.class));

        NotificacionResponse response = notificacionService.marcarLeida(notificacionId);

        assertThat(response).isNotNull();
    }

    // -------- eliminar --------

    @Test
    void eliminar_cuandoEsDueno_borra() {
        autenticarComo(usuario);
        Notificacion n = notificacionDe(usuario, false);
        when(notificacionRepository.findWithUsuarioById(notificacionId)).thenReturn(Optional.of(n));

        notificacionService.eliminar(notificacionId);

        verify(notificacionRepository).delete(n);
    }

    @Test
    void eliminar_cuandoEsAjeno_lanzaAccessDenied() {
        autenticarComo(otroUsuario);
        Notificacion n = notificacionDe(usuario, false);
        when(notificacionRepository.findWithUsuarioById(notificacionId)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificacionService.eliminar(notificacionId))
                .isInstanceOf(AccessDeniedException.class);
    }

    // -------- contarNoLeidas --------

    @Test
    void contarNoLeidas_cuandoEsUsuario_retornaConteo() {
        autenticarComo(usuario);
        when(notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId)).thenReturn(3L);

        long total = notificacionService.contarNoLeidas();

        assertThat(total).isEqualTo(3L);
    }
}