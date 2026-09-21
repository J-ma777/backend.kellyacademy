package com.kellyacademy.calendar.specification;

import com.kellyacademy.calendar.entity.DisponibilidadTutoria;
import org.springframework.data.jpa.domain.Specification;

import java.time.DayOfWeek;
import java.util.UUID;

// Convencion: Specification.unrestricted() cuando el parametro es null.
// Alineado con CursoSpecifications, AsistenciaSpecifications,
// NotificacionSpecifications, AnuncioSpecifications, ConversacionSpecifications,
// MensajeSpecifications, EventoSpecifications.
public final class DisponibilidadTutoriaSpecifications {

    private DisponibilidadTutoriaSpecifications() {
    }

    public static Specification<DisponibilidadTutoria> porDocente(UUID docenteId) {
        if (docenteId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("docente").get("id"), docenteId);
    }

    public static Specification<DisponibilidadTutoria> porDiaSemana(DayOfWeek diaSemana) {
        if (diaSemana == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("diaSemana"), diaSemana);
    }

    public static Specification<DisponibilidadTutoria> porBloqueada(Boolean bloqueada) {
        if (bloqueada == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("bloqueada"), bloqueada);
    }
}