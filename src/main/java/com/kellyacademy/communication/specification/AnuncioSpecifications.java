package com.kellyacademy.communication.specification;

import com.kellyacademy.communication.entity.Anuncio;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class AnuncioSpecifications {

    private AnuncioSpecifications() {
        // Clase de utilidades: no instanciable.
    }

    public static Specification<Anuncio> porCurso(UUID cursoId) {
        if (cursoId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("curso").get("id"), cursoId);
    }

    public static Specification<Anuncio> porActivo(Boolean activo) {
        if (activo == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("activo"), activo);
    }
}