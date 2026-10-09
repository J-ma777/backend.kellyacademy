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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        return crearBatch(List.of(new NuevaNotificacion(usuarioId, tipo, titulo, cuerpo, link)))
                .getFirst();
    }

    public List<NotificacionResponse> crearBatch(List<NuevaNotificacion> destinatarios) {
        if (destinatarios == null || destinatarios.isEmpty()) {
            return List.of();
        }

        for (NuevaNotificacion item : destinatarios) {
            validarUsuarioId(item.usuarioId());
            validarTipo(item.tipo());
            validarTitulo(item.titulo());
            validarCuerpo(item.cuerpo());
            if (item.link() != null && !item.link().isBlank()) {
                validarUrl(item.link());
            }
        }

        LinkedHashSet<UUID> idsUnicos = destinatarios.stream()
                .map(NuevaNotificacion::usuarioId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Usuario> encontrados = usuarioRepository.findAllById(idsUnicos);
        if (encontrados.size() != idsUnicos.size()) {
            Set<UUID> hallados = encontrados.stream()
                    .map(Usuario::getId)
                    .collect(Collectors.toSet());
            UUID faltante = idsUnicos.stream()
                    .filter(id -> !hallados.contains(id))
                    .findFirst()
                    .orElseThrow();
            throw new ResourceNotFoundException("Usuario", "id", faltante);
        }

        Map<UUID, Usuario> usuariosPorId = encontrados.stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));

        List<Notificacion> entidades = new ArrayList<>(destinatarios.size());
        for (NuevaNotificacion item : destinatarios) {
            Notificacion entity = new Notificacion();
            entity.setUsuario(usuariosPorId.get(item.usuarioId()));
            entity.setTipo(item.tipo());
            entity.setTitulo(item.titulo());
            entity.setCuerpo(item.cuerpo());
            entity.setLink(item.link());
            entity.setLeida(false);
            entidades.add(entity);
        }

        return notificacionRepository.saveAll(entidades).stream()
                .map(notificacionMapper::toResponse)
                .toList();
    }

    // Visible para AnuncioService y tests del mismo paquete. No es DTO HTTP.
    record NuevaNotificacion(
            UUID usuarioId,
            TipoNotificacion tipo,
            String titulo,
            String cuerpo,
            String link
    ) {
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

    private void validarUsuarioId(UUID usuarioId) {
        if (usuarioId == null) {
            throw new BusinessException("USUARIO_ID_REQUERIDO", "El ID del usuario es obligatorio.");
        }
    }

    private void validarTipo(TipoNotificacion tipo) {
        if (tipo == null) {
            throw new BusinessException("TIPO_NOTIFICACION_REQUERIDO", "El tipo de notificacion es obligatorio.");
        }
    }

    private void validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new BusinessException("TITULO_REQUERIDO", "El titulo es obligatorio.");
        }
        if (titulo.length() > 200) {
            throw new BusinessException("TITULO_MUY_LARGO", "El titulo no puede exceder 200 caracteres.");
        }
    }

    private void validarCuerpo(String cuerpo) {
        if (cuerpo != null && cuerpo.length() > 500) {
            throw new BusinessException("CUERPO_MUY_LARGO", "El cuerpo no puede exceder 500 caracteres.");
        }
    }

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
