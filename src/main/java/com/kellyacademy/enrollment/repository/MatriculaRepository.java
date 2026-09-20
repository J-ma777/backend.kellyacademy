package com.kellyacademy.enrollment.repository;

import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatriculaRepository extends
        JpaRepository<Matricula, UUID>,
        JpaSpecificationExecutor<Matricula> {

    List<Matricula> findByCursoId(UUID cursoId);

    List<Matricula> findByEstudianteId(UUID estudianteId);

    Optional<Matricula> findByCursoIdAndEstudianteId(UUID cursoId, UUID estudianteId);

    boolean existsByCursoIdAndEstudianteId(UUID cursoId, UUID estudianteId);

    List<Matricula> findByCursoIdAndEstado(UUID cursoId, EstadoMatricula estado);

    // Cuenta matriculas activas del curso. Se usa para validar cupo disponible
    // contra Curso.capacidadMaxima.
    long countByCursoIdAndEstado(UUID cursoId, EstadoMatricula estado);

    // Carga curso y estudiante en la misma query. Necesario para que
    // MatriculaMapper.toResponse lea los escalares sin disparar SELECTs
    // adicionales (open-in-view=false + @ManyToOne LAZY).
    @EntityGraph(attributePaths = {"curso", "estudiante"})
    @Query("SELECT m FROM Matricula m WHERE m.id = :id")
    Optional<Matricula> findWithCursoAndEstudianteById(@Param("id") UUID id);

    // Sobrescribe el findAll de JpaSpecificationExecutor para aplicar @EntityGraph.
    // Mismo patron que CursoRepository.
    @Override
    @EntityGraph(attributePaths = {"curso", "estudiante"})
    Page<Matricula> findAll(@Nullable Specification<Matricula> spec, Pageable pageable);
}