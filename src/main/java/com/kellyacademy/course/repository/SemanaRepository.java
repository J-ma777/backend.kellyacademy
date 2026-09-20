package com.kellyacademy.course.repository;

import com.kellyacademy.course.entity.Semana;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SemanaRepository extends JpaRepository<Semana, UUID> {

    List<Semana> findByUnidadIdOrderByNumeroAsc(UUID unidadId);

    Optional<Semana> findByUnidadIdAndNumero(UUID unidadId, Integer numero);

    Optional<Semana> findByUnidadIdAndEsActualTrue(UUID unidadId);

    boolean existsByUnidadId(UUID unidadId);

    boolean existsByUnidadIdAndNumero(UUID unidadId, Integer numero);

    // Carga unidad + curso + docente en la misma query. Necesario para SemanaMapper
    // (lee unidad.getId()) y para validar propietario del curso en el servicio.
    @EntityGraph(attributePaths = {"unidad", "unidad.curso", "unidad.curso.docente"})
    @Query("SELECT s FROM Semana s WHERE s.id = :id")
    Optional<Semana> findWithUnidadCursoDocenteById(@Param("id") UUID id);
}