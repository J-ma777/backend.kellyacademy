package com.kellyacademy.course.repository;

import com.kellyacademy.course.entity.Clase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaseRepository extends JpaRepository<Clase, UUID> {

    List<Clase> findBySemanaIdOrderByFechaHoraAsc(UUID semanaId);

    List<Clase> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    boolean existsBySemanaId(UUID semanaId);

    // Carga el grafo completo hasta el docente del curso para que ClaseMapper
    // (lee semana.getId()) y la validacion de autorizacion (semana.unidad.curso.docente.id)
    // funcionen sin LazyInitializationException con open-in-view=false.
    @EntityGraph(attributePaths = {
            "semana",
            "semana.unidad",
            "semana.unidad.curso",
            "semana.unidad.curso.docente"
    })
    @Query("SELECT c FROM Clase c WHERE c.id = :id")
    Optional<Clase> findWithSemanaCursoDocenteById(@Param("id") UUID id);
}