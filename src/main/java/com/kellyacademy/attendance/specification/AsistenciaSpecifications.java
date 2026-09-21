package com.kellyacademy.attendance.specification;

import com.kellyacademy.attendance.entity.Asistencia;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class AsistenciaSpecifications {

    private AsistenciaSpecifications() {
        // Clase de utilidades: no instanciable.
    }

    // Filtra asistencias por clase. Match-all cuando claseId es null.
    public static Specification<Asistencia> porClase(UUID claseId) {
        if (claseId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("clase").get("id"), claseId);
    }

    // Filtra asistencias por docente dueno del curso de la clase.
    // Match-all cuando docenteId es null.
    public static Specification<Asistencia> porDocente(UUID docenteId) {
        if (docenteId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(
                root.get("clase").get("semana").get("unidad").get("curso").get("docente").get("id"),
                docenteId
        );
    }

    // Filtra asistencias por estudiante. Match-all cuando estudianteId es null.
    public static Specification<Asistencia> porEstudiante(UUID estudianteId) {
        if (estudianteId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("estudiante").get("id"), estudianteId);
    }
}