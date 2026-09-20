package com.kellyacademy.enrollment.specification;

import com.kellyacademy.enrollment.entity.Entrega;
import com.kellyacademy.enrollment.enums.EstadoEntrega;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class EntregaSpecifications {

    private EntregaSpecifications() {
    }

    public static Specification<Entrega> porTareaId(UUID tareaId) {
        return (root, query, cb) ->
                tareaId == null ? null : cb.equal(root.get("tarea").get("id"), tareaId);
    }

    public static Specification<Entrega> porEstudianteId(UUID estudianteId) {
        return (root, query, cb) ->
                estudianteId == null ? null : cb.equal(root.get("estudiante").get("id"), estudianteId);
    }

    public static Specification<Entrega> porEstado(EstadoEntrega estado) {
        return (root, query, cb) ->
                estado == null ? null : cb.equal(root.get("estado"), estado);
    }

    // Filtra entregas cuyo curso de la tarea pertenezca al docente dado.
    // Se usa para que un DOCENTE solo vea las entregas de sus cursos.
    public static Specification<Entrega> porDocenteId(UUID docenteId) {
        return (root, query, cb) ->
                docenteId == null ? null
                        : cb.equal(
                        root.get("tarea").get("semana").get("unidad").get("curso").get("docente").get("id"),
                        docenteId
                );
    }
}