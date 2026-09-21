package com.kellyacademy.library.specification;

import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.enums.TipoMaterial;
import com.kellyacademy.library.entity.RecursoBiblioteca;
import org.springframework.data.jpa.domain.Specification;

// Convencion: Specification.unrestricted() cuando el parametro es null.
// Alineado con CursoSpecifications, AsistenciaSpecifications, NotificacionSpecifications,
// AnuncioSpecifications, ConversacionSpecifications, MensajeSpecifications,
// EventoSpecifications, DisponibilidadTutoriaSpecifications, TutoriaSpecifications.
public final class RecursoBibliotecaSpecifications {

    private RecursoBibliotecaSpecifications() {
    }

    public static Specification<RecursoBiblioteca> porCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("categoria")), categoria.toLowerCase());
    }

    public static Specification<RecursoBiblioteca> porNivelCefr(NivelCefr nivelCefr) {
        if (nivelCefr == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("nivelCefr"), nivelCefr);
    }

    public static Specification<RecursoBiblioteca> porTipo(TipoMaterial tipo) {
        if (tipo == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("tipo"), tipo);
    }

    public static Specification<RecursoBiblioteca> tituloContiene(String texto) {
        if (texto == null || texto.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("titulo")), "%" + texto.toLowerCase() + "%");
    }
}