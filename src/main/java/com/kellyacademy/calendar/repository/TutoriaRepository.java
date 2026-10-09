package com.kellyacademy.calendar.repository;

import com.kellyacademy.calendar.entity.Tutoria;
import com.kellyacademy.calendar.enums.EstadoTutoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TutoriaRepository extends
        JpaRepository<Tutoria, UUID>,
        JpaSpecificationExecutor<Tutoria> {

    List<Tutoria> findByEstudianteIdOrderByFechaDesc(UUID estudianteId);

    List<Tutoria> findByDocenteIdOrderByFechaDesc(UUID docenteId);

    List<Tutoria> findByDocenteIdAndFechaBetween(UUID docenteId, LocalDateTime inicio, LocalDateTime fin);

    List<Tutoria> findByEstado(EstadoTutoria estado);

    // Carga estudiante, docente y curso en la misma query. Necesario para que
    // TutoriaMapper lea los escalares sin disparar SELECTs adicionales
    // (open-in-view=false + @ManyToOne LAZY).
    @EntityGraph(attributePaths = {"estudiante", "docente", "curso"})
    @Query("SELECT t FROM Tutoria t WHERE t.id = :id")
    Optional<Tutoria> findWithEstudianteDocenteCursoById(@Param("id") UUID id);

    // Sobrescribe el findAll de JpaSpecificationExecutor para aplicar @EntityGraph.
    // Mismo patron que CursoRepository, MatriculaRepository, EventoRepository
    // y DisponibilidadTutoriaRepository.
    @Override
    @EntityGraph(attributePaths = {"estudiante", "docente", "curso"})
    Page<Tutoria> findAll(@Nullable Specification<Tutoria> spec, Pageable pageable);

    // Slice #48: busca tutorias CONFIRMADAS candidatas para solape por docente o estudiante.
    // El filtro fino de solape de intervalos [inicio, fin] x [t.fecha, t.fecha + duracion]
    // se realiza en TutoriaService para maxima portabilidad entre H2 y PostgreSQL.
    @Query("""
            SELECT t FROM Tutoria t
            WHERE t.estado = com.kellyacademy.calendar.enums.EstadoTutoria.CONFIRMADA
              AND (:excluirId IS NULL OR t.id <> :excluirId)
              AND (t.docente.id = :docenteId OR t.estudiante.id = :estudianteId)
              AND t.fecha >= :ventanaInicio
              AND t.fecha < :fin
            """)
    List<Tutoria> findConfirmadasCandidatasSolape(
            @Param("docenteId") UUID docenteId,
            @Param("estudianteId") UUID estudianteId,
            @Param("ventanaInicio") LocalDateTime ventanaInicio,
            @Param("fin") LocalDateTime fin,
            @Param("excluirId") UUID excluirId
    );
}
