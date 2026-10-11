package com.kellyacademy.enrollment.specification;

import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/*
 Specifications para filtros combinables de Matricula.
 Cada metodo devuelve Specification.unrestricted() cuando el filtro no aplica,
 de forma que allOf(...) los trata como "sin restriccion".
 */
public final class MatriculaSpecifications {

    private MatriculaSpecifications() {
        // Clase de utilidades: no instanciable.
    }

    public static Specification<Matricula> porCursoId(UUID cursoId) {
        if (cursoId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("curso").get("id"), cursoId);
    }

    public static Specification<Matricula> porEstudianteId(UUID estudianteId) {
        if (estudianteId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("estudiante").get("id"), estudianteId);
    }

    public static Specification<Matricula> porEstado(EstadoMatricula estado) {
        if (estado == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }
}