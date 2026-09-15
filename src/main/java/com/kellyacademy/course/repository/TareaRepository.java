package com.kellyacademy.course.repository;

import com.kellyacademy.course.entity.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TareaRepository extends JpaRepository<Tarea, UUID> {

    List<Tarea> findBySemanaIdOrderByFechaLimiteAsc(UUID semanaId);

    List<Tarea> findByFechaLimiteBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Tarea> findByFechaLimiteBefore(LocalDateTime fecha);
}