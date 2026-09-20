package com.kellyacademy.course.repository;

import com.kellyacademy.course.entity.Tarea;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TareaRepository extends JpaRepository<Tarea, UUID> {

    List<Tarea> findBySemanaIdOrderByFechaLimiteAsc(UUID semanaId);

    List<Tarea> findByFechaLimiteBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Tarea> findByFechaLimiteBefore(LocalDateTime fecha);

    boolean existsBySemanaId(UUID semanaId);

    @EntityGraph(attributePaths = {
            "semana",
            "semana.unidad",
            "semana.unidad.curso",
            "semana.unidad.curso.docente"
    })
    @Query("SELECT t FROM Tarea t WHERE t.id = :id")
    Optional<Tarea> findWithSemanaCursoDocenteById(@Param("id") UUID id);
}