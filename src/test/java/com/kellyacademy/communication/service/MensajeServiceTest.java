package com.kellyacademy.communication.service;

import com.kellyacademy.communication.dto.request.CrearMensajeRequest;
import com.kellyacademy.communication.dto.response.MensajeResponse;
import com.kellyacademy.communication.entity.Conversacion;
import com.kellyacademy.communication.entity.Mensaje;
import com.kellyacademy.communication.enums.TipoNotificacion;
import com.kellyacademy.communication.mapper.MensajeMapper;
import com.kellyacademy.communication.repository.ConversacionRepository;
import com.kellyacademy.communication.repository.MensajeRepository;
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MensajeServiceTest {

    @Mock private MensajeRepository mensajeRepository;
    @Mock private ConversacionRepository conversacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MensajeMapper mensajeMapper;
    @Mock private NotificacionService notificacionService;

    @InjectMocks private MensajeService mensajeService;

    private UUID autenticadoId;
    private UUID otroId;
    private UUID conversacionId;
    private Usuario autenticado;
    private Usuario otro;
    private Conversacion conversacion;

    @BeforeEach
    void setUp() {
        autenticadoId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        otroId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        conversacionId = UUID.randomUUID();

        Rol rol = new Rol();
        rol.setNombre("ESTUDIANTE");

        autenticado = new Usuario();
        autenticado.setId(autenticadoId);
        autenticado.setRoles(Set.of(rol));

        otro = new Usuario();
        otro.setId(otroId);
        otro.setRoles(Set.of(rol));

        conversacion = new Conversacion();
        conversacion.setId(conversacionId);
        conversacion.setParticipante1(autenticado);
        conversacion.setParticipante2(otro);
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
    void crear_cuandoEsParticipante_creaYNotifica() {
        autenticarComo(autenticado);
        CrearMensajeRequest request = new CrearMensajeRequest("Hola", null);

        Mensaje guardado = new Mensaje();
        guardado.setId(UUID.randomUUID());
        guardado.setConversacion(conversacion);
        guardado.setRemitente(autenticado);

        when(conversacionRepository.findWithParticipantesById(conversacionId)).thenReturn(Optional.of(conversacion));
        when(usuarioRepository.findById(autenticadoId)).thenReturn(Optional.of(autenticado));
        when(mensajeMapper.toEntity(request)).thenReturn(new Mensaje());
        when(mensajeRepository.save(any(Mensaje.class))).thenReturn(guardado);
        when(mensajeMapper.toResponse(guardado)).thenReturn(mock(MensajeResponse.class));

        MensajeResponse response = mensajeService.crear(conversacionId, request);

        assertThat(response).isNotNull();
        verify(notificacionService).crear(eq(otroId), eq(TipoNotificacion.MENSAJE), any(), any(), any());
    }

    @Test
    void crear_cuandoNoEsParticipante_lanzaAccessDenied() {
        Usuario ajeno = new Usuario();
        ajeno.setId(UUID.randomUUID());
        Rol rol = new Rol();
        rol.setNombre("ESTUDIANTE");
        ajeno.setRoles(Set.of(rol));
        autenticarComo(ajeno);

        CrearMensajeRequest request = new CrearMensajeRequest("Hola", null);

        when(conversacionRepository.findWithParticipantesById(conversacionId)).thenReturn(Optional.of(conversacion));

        assertThatThrownBy(() -> mensajeService.crear(conversacionId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void crear_cuandoConversacionNoExiste_lanzaResourceNotFound() {
        autenticarComo(autenticado);
        CrearMensajeRequest request = new CrearMensajeRequest("Hola", null);

        when(conversacionRepository.findWithParticipantesById(conversacionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mensajeService.crear(conversacionId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}