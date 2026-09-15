package com.kellyacademy.calendar.repository;

import com.kellyacademy.calendar.entity.DisponibilidadTutoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface DisponibilidadTutoriaRepository extends JpaRepository<DisponibilidadTutoria, UUID> {

    List<DisponibilidadTutoria> findByDocenteId(UUID docenteId);

    List<DisponibilidadTutoria> findByDocenteIdAndDiaSemana(UUID docenteId, DayOfWeek diaSemana);

    List<DisponibilidadTutoria> findByDocenteIdAndBloqueadaFalse(UUID docenteId);
}