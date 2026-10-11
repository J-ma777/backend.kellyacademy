package com.kellyacademy.enrollment.specification;

import com.kellyacademy.enrollment.entity.Entrega;
import com.kellyacademy.enrollment.enums.EstadoEntrega;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/*
 Specifications para filtros combinables de Entrega.
 Cada metodo devuelve Specification.unrestricted() cuando el filtro no aplica,
 de forma que allOf(...) los trata como "sin restriccion".
 */
public final class EntregaSpecifications {

    private EntregaSpecifications() {
        // Clase de utilidades: no instanciable.
    }

    public static Specification<Entrega> porTareaId(UUID tareaId) {
        if (tareaId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("tarea").get("id"), tareaId);
    }

    public static Specification<Entrega> porEstudianteId(UUID estudianteId) {
        if (estudianteId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("estudiante").get("id"), estudianteId);
    }

    public static Specification<Entrega> porEstado(EstadoEntrega estado) {
        if (estado == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    // Filtra entregas cuyo curso de la tarea pertenezca al docente dado.
    // Se usa para que un DOCENTE solo vea las entregas de sus cursos.
    public static Specification<Entrega> porDocenteId(UUID docenteId) {
        if (docenteId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(
                root.get("tarea").get("semana").get("unidad").get("curso").get("docente").get("id"),
                docenteId
        );
    }
}