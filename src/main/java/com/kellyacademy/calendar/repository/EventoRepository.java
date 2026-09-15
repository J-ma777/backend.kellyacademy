package com.kellyacademy.calendar.repository;

import com.kellyacademy.calendar.entity.Evento;
import com.kellyacademy.calendar.enums.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

    List<Evento> findByUsuarioIdOrderByInicioAsc(UUID usuarioId);

    List<Evento> findByUsuarioIdAndInicioBetween(UUID usuarioId, LocalDateTime inicio, LocalDateTime fin);

    List<Evento> findByCursoId(UUID cursoId);

    List<Evento> findByUsuarioIdAndTipo(UUID usuarioId, TipoEvento tipo);
}