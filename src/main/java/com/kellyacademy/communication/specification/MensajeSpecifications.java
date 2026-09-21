package com.kellyacademy.communication.specification;

import com.kellyacademy.communication.entity.Mensaje;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class MensajeSpecifications {

    private MensajeSpecifications() {
        // Clase de utilidades: no instanciable.
    }

    public static Specification<Mensaje> porConversacion(UUID conversacionId) {
        if (conversacionId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("conversacion").get("id"), conversacionId);
    }
}