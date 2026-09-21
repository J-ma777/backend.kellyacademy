package com.kellyacademy.communication.specification;

import com.kellyacademy.communication.entity.Conversacion;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class ConversacionSpecifications {

    private ConversacionSpecifications() {
        // Clase de utilidades: no instanciable.
    }

    // Conversaciones donde el usuario es participante 1 o 2.
    public static Specification<Conversacion> porParticipante(UUID usuarioId) {
        if (usuarioId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("participante1").get("id"), usuarioId),
                cb.equal(root.get("participante2").get("id"), usuarioId)
        );
    }

    public static Specification<Conversacion> porCurso(UUID cursoId) {
        if (cursoId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("curso").get("id"), cursoId);
    }
}