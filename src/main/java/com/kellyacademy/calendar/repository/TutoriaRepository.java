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
}