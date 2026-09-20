package com.kellyacademy.enrollment.specification;

import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class MatriculaSpecifications {

    private MatriculaSpecifications() {
    }

    public static Specification<Matricula> porCursoId(UUID cursoId) {
        return (root, query, cb) ->
                cursoId == null ? null : cb.equal(root.get("curso").get("id"), cursoId);
    }

    public static Specification<Matricula> porEstudianteId(UUID estudianteId) {
        return (root, query, cb) ->
                estudianteId == null ? null : cb.equal(root.get("estudiante").get("id"), estudianteId);
    }

    public static Specification<Matricula> porEstado(EstadoMatricula estado) {
        return (root, query, cb) ->
                estado == null ? null : cb.equal(root.get("estado"), estado);
    }
}