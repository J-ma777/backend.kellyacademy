package com.kellyacademy.communication.specification;

import com.kellyacademy.communication.entity.Notificacion;
import com.kellyacademy.communication.enums.TipoNotificacion;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class NotificacionSpecifications {

    private NotificacionSpecifications() {
        // Clase de utilidades: no instanciable.
    }

    public static Specification<Notificacion> porUsuario(UUID usuarioId) {
        if (usuarioId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("usuario").get("id"), usuarioId);
    }

    public static Specification<Notificacion> porLeida(Boolean leida) {
        if (leida == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("leida"), leida);
    }

    public static Specification<Notificacion> porTipo(TipoNotificacion tipo) {
        if (tipo == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("tipo"), tipo);
    }
}