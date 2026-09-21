package com.kellyacademy.calendar.specification;

import com.kellyacademy.calendar.entity.Tutoria;
import com.kellyacademy.calendar.enums.EstadoTutoria;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

// Convencion: Specification.unrestricted() cuando el parametro es null.
// Alineado con CursoSpecifications, AsistenciaSpecifications, NotificacionSpecifications,
// AnuncioSpecifications, ConversacionSpecifications, MensajeSpecifications,
// EventoSpecifications, DisponibilidadTutoriaSpecifications.
public final class TutoriaSpecifications {

    private TutoriaSpecifications() {
    }

    public static Specification<Tutoria> porEstudiante(UUID estudianteId) {
        if (estudianteId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("estudiante").get("id"), estudianteId);
    }

    public static Specification<Tutoria> porDocente(UUID docenteId) {
        if (docenteId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("docente").get("id"), docenteId);
    }

    public static Specification<Tutoria> porCurso(UUID cursoId) {
        if (cursoId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("curso").get("id"), cursoId);
    }

    public static Specification<Tutoria> porEstado(EstadoTutoria estado) {
        if (estado == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<Tutoria> fechaDesde(LocalDateTime desde) {
        if (desde == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), desde);
    }

    public static Specification<Tutoria> fechaHasta(LocalDateTime hasta) {
        if (hasta == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), hasta);
    }

    // Filtra tutorias donde el autenticado sea estudiante o docente.
    // Se usa para el listado de no-admin: solo ve las suyas.
    public static Specification<Tutoria> involucraA(UUID usuarioId) {
        if (usuarioId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("estudiante").get("id"), usuarioId),
                cb.equal(root.get("docente").get("id"), usuarioId)
        );
    }
}