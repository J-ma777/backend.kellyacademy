package com.kellyacademy.communication.service;

import com.kellyacademy.communication.dto.response.NotificacionResponse;
import com.kellyacademy.communication.dto.response.NotificacionResumenResponse;
import com.kellyacademy.communication.entity.Notificacion;
import com.kellyacademy.communication.enums.TipoNotificacion;
import com.kellyacademy.communication.mapper.NotificacionMapper;
import com.kellyacademy.communication.repository.NotificacionRepository;
import com.kellyacademy.communication.specification.NotificacionSpecifications;
import com.kellyacademy.shared.exception.BusinessException;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionMapper notificacionMapper;

    // -------- listar --------

    @Transactional(readOnly = true)
    public Page<NotificacionResumenResponse> listar(Boolean leida, TipoNotificacion tipo, Pageable pageable) {
        UUID usuarioFiltro = SecurityUtils.esAdmin() ? null : SecurityUtils.getUsuarioAutenticadoId();

        Specification<Notificacion> spec = Specification.allOf(
                NotificacionSpecifications.porUsuario(usuarioFiltro),
                NotificacionSpecifications.porLeida(leida),
                NotificacionSpecifications.porTipo(tipo)
        );

        return notificacionRepository.findAll(spec, pageable)
                .map(notificacionMapper::toResumenResponse);
    }

    @Transactional(readOnly = true)
    public Page<NotificacionResumenResponse> listarNoLeidas(Pageable pageable) {
        UUID usuarioId = SecurityUtils.getUsuarioAutenticadoId();
        Specification<Notificacion> spec = Specification.allOf(
                NotificacionSpecifications.porUsuario(usuarioId),
                NotificacionSpecifications.porLeida(false)
        );
        return notificacionRepository.findAll(spec, pageable)
                .map(notificacionMapper::toResumenResponse);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas() {
        UUID usuarioId = SecurityUtils.getUsuarioAutenticadoId();
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    // -------- obtener --------

    @Transactional(readOnly = true)
    public NotificacionResponse obtener(UUID id) {
        Notificacion notificacion = notificacionRepository.findWithUsuarioById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion", "id", id));

        validarPuedeConsultar(notificacion);
        return notificacionMapper.toResponse(notificacion);
    }

    // -------- marcar leida --------

    public NotificacionResponse marcarLeida(UUID id) {
        Notificacion notificacion = notificacionRepository.findWithUsuarioById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion", "id", id));

        validarPuedeModificar(notificacion);

        // Idempotente: si ya esta leida, no falla.
        if (!Boolean.TRUE.equals(notificacion.getLeida())) {
            notificacion.setLeida(true);
        }

        return notificacionMapper.toResponse(notificacion);
    }

    public void marcarTodasLeidas() {
        UUID usuarioId = SecurityUtils.getUsuarioAutenticadoId();
        notificacionRepository.marcarTodasLeidasPorUsuario(usuarioId);
    }

    // -------- eliminar --------

    public void eliminar(UUID id) {
        Notificacion notificacion = notificacionRepository.findWithUsuarioById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion", "id", id));

        validarPuedeModificar(notificacion);
        notificacionRepository.delete(notificacion);
    }

    // -------- crear (interno, para otros servicios) --------

    // Sin endpoint HTTP. Invocado por AnuncioService, MensajeService,
    // CalificacionService, etc. NO valida auto-notificacion: esa regla la
    // impone el llamador. NO valida rol ni permisos: es infraestructura.
    public NotificacionResponse crear(UUID usuarioId,
                                      TipoNotificacion tipo,
                                      String titulo,
                                      String cuerpo,
                                      String link) {
        if (usuarioId == null) {
            throw new BusinessException("USUARIO_ID_REQUERIDO", "El ID del usuario es obligatorio.");
        }
        if (tipo == null) {
            throw new BusinessException("TIPO_NOTIFICACION_REQUERIDO", "El tipo de notificacion es obligatorio.");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new BusinessException("TITULO_REQUERIDO", "El titulo es obligatorio.");
        }
        if (titulo.length() > 200) {
            throw new BusinessException("TITULO_MUY_LARGO", "El titulo no puede exceder 200 caracteres.");
        }
        if (cuerpo != null && cuerpo.length() > 500) {
            throw new BusinessException("CUERPO_MUY_LARGO", "El cuerpo no puede exceder 500 caracteres.");
        }
        if (link != null && !link.isBlank()) {
            validarUrl(link);
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", usuarioId));

        Notificacion entity = new Notificacion();
        entity.setUsuario(usuario);
        entity.setTipo(tipo);
        entity.setTitulo(titulo);
        entity.setCuerpo(cuerpo);
        entity.setLink(link);
        entity.setLeida(false);

        Notificacion guardada = notificacionRepository.save(entity);
        return notificacionMapper.toResponse(guardada);
    }

    // -------- autorizacion --------

    private void validarPuedeConsultar(Notificacion notificacion) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        if (!notificacion.getUsuario().getId().equals(SecurityUtils.getUsuarioAutenticadoId())) {
            throw new AccessDeniedException("No tiene permiso para consultar esta notificacion.");
        }
    }

    private void validarPuedeModificar(Notificacion notificacion) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        if (!notificacion.getUsuario().getId().equals(SecurityUtils.getUsuarioAutenticadoId())) {
            throw new AccessDeniedException("No tiene permiso para modificar esta notificacion.");
        }
    }

    // -------- helpers --------

    private void validarUrl(String url) {
        // Acepta dos formatos validos para "link":
        // 1) Ruta relativa interna que empieza con "/" (ej. /api/anuncios/<uuid>).
        //    Es el formato que usan los servicios internos al crear notificaciones.
        // 2) URL absoluta (http://, https://...) con scheme + host.
        // Rechaza cualquier otro formato (texto suelto, espacios, scheme sin host).
        if (url.startsWith("/")) {
            if (url.contains(" ") || url.contains("\t") || url.contains("\n")) {
                throw new BusinessException("URL_INVALIDA", "La ruta del link no es valida.");
            }
            return;
        }
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new BusinessException("URL_INVALIDA", "La URL del link no es valida.");
            }
        } catch (URISyntaxException e) {
            throw new BusinessException("URL_INVALIDA", "La URL del link no es valida.");
        }
    }
}