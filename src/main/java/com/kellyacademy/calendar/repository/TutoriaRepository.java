package com.kellyacademy.calendar.repository;

import com.kellyacademy.calendar.entity.Tutoria;
import com.kellyacademy.calendar.enums.EstadoTutoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TutoriaRepository extends JpaRepository<Tutoria, UUID> {

    List<Tutoria> findByEstudianteIdOrderByFechaDesc(UUID estudianteId);

    List<Tutoria> findByDocenteIdOrderByFechaDesc(UUID docenteId);

    List<Tutoria> findByDocenteIdAndFechaBetween(UUID docenteId, LocalDateTime inicio, LocalDateTime fin);

    List<Tutoria> findByEstado(EstadoTutoria estado);
}