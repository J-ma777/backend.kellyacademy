package com.kellyacademy.calendar.repository;

import com.kellyacademy.calendar.entity.DisponibilidadTutoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisponibilidadTutoriaRepository extends
        JpaRepository<DisponibilidadTutoria, UUID>,
        JpaSpecificationExecutor<DisponibilidadTutoria> {

    List<DisponibilidadTutoria> findByDocenteId(UUID docenteId);

    List<DisponibilidadTutoria> findByDocenteIdAndDiaSemana(UUID docenteId, DayOfWeek diaSemana);

    List<DisponibilidadTutoria> findByDocenteIdAndBloqueadaFalse(UUID docenteId);

    // Carga el docente en la misma query. Necesario para que DisponibilidadMapper
    // lea docente.nombre y docente.apellido sin disparar SELECT adicional
    // (open-in-view=false + @ManyToOne LAZY).
    @EntityGraph(attributePaths = {"docente"})
    @Query("SELECT d FROM DisponibilidadTutoria d WHERE d.id = :id")
    Optional<DisponibilidadTutoria> findWithDocenteById(@Param("id") UUID id);

    // Sobrescribe el findAll de JpaSpecificationExecutor para aplicar @EntityGraph.
    // Mismo patron que CursoRepository, MatriculaRepository y EventoRepository.
    @Override
    @EntityGraph(attributePaths = {"docente"})
    Page<DisponibilidadTutoria> findAll(@Nullable Specification<DisponibilidadTutoria> spec, Pageable pageable);

    // Deuda #40: detecta solape de bloques del mismo docente y dia.
    // Interseccion de intervalos [inicio, fin] con [horaInicio, horaFin]:
    //   existing.horaInicio < nuevo.horaFin  AND  existing.horaFin > nuevo.horaInicio
    // El parametro excluirId es opcional (null en crear, id propio en actualizar).
    // NO filtra por bloqueada: el bloque sigue existiendo en la franja horaria,
    // el flag es solo informativo para el frontend.
    @Query("""
            SELECT d FROM DisponibilidadTutoria d
            WHERE d.docente.id = :docenteId
              AND d.diaSemana = :diaSemana
              AND d.horaInicio < :horaFin
              AND d.horaFin > :horaInicio
              AND (:excluirId IS NULL OR d.id <> :excluirId)
            """)
    List<DisponibilidadTutoria> findSolapadas(
            @Param("docenteId") UUID docenteId,
            @Param("diaSemana") DayOfWeek diaSemana,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("excluirId") UUID excluirId
    );
}