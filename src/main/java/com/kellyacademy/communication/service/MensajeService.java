package com.kellyacademy.communication.service;

import com.kellyacademy.communication.dto.request.CrearMensajeRequest;
import com.kellyacademy.communication.dto.response.MensajeResponse;
import com.kellyacademy.communication.dto.response.MensajeResumenResponse;
import com.kellyacademy.communication.entity.Conversacion;
import com.kellyacademy.communication.entity.Mensaje;
import com.kellyacademy.communication.enums.TipoNotificacion;
import com.kellyacademy.communication.mapper.MensajeMapper;
import com.kellyacademy.communication.repository.ConversacionRepository;
import com.kellyacademy.communication.repository.MensajeRepository;
import com.kellyacademy.communication.specification.MensajeSpecifications;
import com.kellyacademy.shared.config.AppTime;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.shared.util.SecurityUtils;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final ConversacionRepository conversacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final MensajeMapper mensajeMapper;
    private final NotificacionService notificacionService;

    // -------- listar --------

    @Transactional(readOnly = true)
    public Page<MensajeResumenResponse> listar(UUID conversacionId, Pageable pageable) {
        Conversacion conversacion = conversacionRepository.findWithParticipantesById(conversacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversacion", "id", conversacionId));

        validarParticipanteOAdmin(conversacion);

        Specification<Mensaje> spec = Specification.allOf(
                MensajeSpecifications.porConversacion(conversacionId)
        );

        return mensajeRepository.findAll(spec, pageable)
                .map(mensajeMapper::toResumenResponse);
    }

    // -------- obtener --------

    @Transactional(readOnly = true)
    public MensajeResponse obtener(UUID id) {
        Mensaje mensaje = mensajeRepository.findWithConversacionAndRemitenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje", "id", id));

        validarParticipanteOAdmin(mensaje.getConversacion());
        return mensajeMapper.toResponse(mensaje);
    }

    // -------- crear --------

    public MensajeResponse crear(UUID conversacionId, CrearMensajeRequest request) {
        Conversacion conversacion = conversacionRepository.findWithParticipantesById(conversacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversacion", "id", conversacionId));

        // Deuda #33: valida que el autenticado sea participante antes de insertar.
        validarParticipante(conversacion);

        UUID autenticadoId = SecurityUtils.getUsuarioAutenticadoId();
        Usuario remitente = usuarioRepository.findById(autenticadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", autenticadoId));

        LocalDateTime ahora = LocalDateTime.now(AppTime.ZONA_NEGOCIO);

        Mensaje entity = mensajeMapper.toEntity(request);
        entity.setConversacion(conversacion);
        entity.setRemitente(remitente);
        entity.setEnviadoAt(ahora);
        entity.setLeido(false);

        Mensaje guardado = mensajeRepository.save(entity);

        // Actualiza ultimoMensajeAt de la conversacion.
        conversacion.setUltimoMensajeAt(ahora);

        // Notifica al otro participante.
        UUID destinatarioId = conversacion.getParticipante1().getId().equals(autenticadoId)
                ? conversacion.getParticipante2().getId()
                : conversacion.getParticipante1().getId();

        notificacionService.crear(
                destinatarioId,
                TipoNotificacion.MENSAJE,
                "Nuevo mensaje",
                null,
                "/api/conversaciones/" + conversacionId
        );

        return mensajeMapper.toResponse(guardado);
    }

    // -------- marcar leido --------

    public MensajeResponse marcarLeido(UUID conversacionId, UUID mensajeId) {
        Mensaje mensaje = mensajeRepository.findWithConversacionAndRemitenteById(mensajeId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje", "id", mensajeId));

        if (!mensaje.getConversacion().getId().equals(conversacionId)) {
            throw new ResourceNotFoundException("Mensaje", "id", mensajeId);
        }

        validarParticipanteOAdmin(mensaje.getConversacion());

        // Idempotente.
        if (!Boolean.TRUE.equals(mensaje.getLeido())) {
            mensaje.setLeido(true);
        }

        return mensajeMapper.toResponse(mensaje);
    }

    // -------- leer todos --------

    public void marcarTodosLeidos(UUID conversacionId) {
        Conversacion conversacion = conversacionRepository.findWithParticipantesById(conversacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversacion", "id", conversacionId));

        validarParticipante(conversacion);

        UUID autenticadoId = SecurityUtils.getUsuarioAutenticadoId();
        mensajeRepository.marcarTodosLeidosPorUsuario(conversacionId, autenticadoId);
    }

    // -------- eliminar --------

    public void eliminar(UUID id) {
        Mensaje mensaje = mensajeRepository.findWithConversacionAndRemitenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje", "id", id));

        mensajeRepository.delete(mensaje);
    }

    // -------- helpers --------

    private void validarParticipante(Conversacion conversacion) {
        UUID autenticadoId = SecurityUtils.getUsuarioAutenticadoId();
        boolean esParticipante = conversacion.getParticipante1().getId().equals(autenticadoId)
                || conversacion.getParticipante2().getId().equals(autenticadoId);
        if (!esParticipante) {
            throw new AccessDeniedException("No tiene permiso para acceder a esta conversacion.");
        }
    }

    private void validarParticipanteOAdmin(Conversacion conversacion) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        validarParticipante(conversacion);
    }
}