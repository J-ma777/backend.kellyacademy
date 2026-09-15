package com.kellyacademy.course.repository;

import com.kellyacademy.course.entity.Clase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ClaseRepository extends JpaRepository<Clase, UUID> {

    List<Clase> findBySemanaIdOrderByFechaHoraAsc(UUID semanaId);

    List<Clase> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);
}