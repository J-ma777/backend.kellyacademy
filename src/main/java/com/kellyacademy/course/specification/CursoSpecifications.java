package com.kellyacademy.course.specification;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.enums.NivelCefr;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/*
 Specifications para filtros combinables de Curso.
 Cada metodo devuelve Specification.unrestricted() cuando el filtro no aplica,
 de forma que allOf(...) los trata como "sin restriccion" sin romper.
 */
public final class CursoSpecifications {

    private CursoSpecifications() {
    }

    public static Specification<Curso> porEstado(EstadoCurso estado) {
        if (estado == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<Curso> porNivelCefr(NivelCefr nivelCefr) {
        if (nivelCefr == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("nivelCefr"), nivelCefr);
    }

    public static Specification<Curso> porDocenteId(UUID docenteId) {
        if (docenteId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("docente").get("id"), docenteId);
    }

    public static Specification<Curso> tituloContiene(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return Specification.unrestricted();
        }
        String patron = "%" + titulo.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("titulo")), patron);
    }
}