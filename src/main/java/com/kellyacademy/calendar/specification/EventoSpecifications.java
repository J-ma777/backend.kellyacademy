package com.kellyacademy.calendar.specification;

import com.kellyacademy.calendar.entity.Evento;
import com.kellyacademy.calendar.enums.TipoEvento;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

// Convencion: Specification.unrestricted() cuando el parametro es null.
// Alineado con CursoSpecifications, AsistenciaSpecifications,
// NotificacionSpecifications, AnuncioSpecifications, ConversacionSpecifications,
// MensajeSpecifications. (Deuda #65: Matricula/Entrega quedaron con null.)
public final class EventoSpecifications {

    private EventoSpecifications() {
    }

    public static Specification<Evento> porUsuario(UUID usuarioId) {
        if (usuarioId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("usuario").get("id"), usuarioId);
    }

    public static Specification<Evento> porCurso(UUID cursoId) {
        if (cursoId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("curso").get("id"), cursoId);
    }

    public static Specification<Evento> porTipo(TipoEvento tipo) {
        if (tipo == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("tipo"), tipo);
    }

    public static Specification<Evento> inicioDesde(LocalDateTime desde) {
        if (desde == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("inicio"), desde);
    }

    public static Specification<Evento> inicioHasta(LocalDateTime hasta) {
        if (hasta == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("inicio"), hasta);
    }
}