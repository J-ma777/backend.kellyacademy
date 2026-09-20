package com.kellyacademy.course.repository;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.enums.NivelCefr;
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

public interface CursoRepository extends
        JpaRepository<Curso, UUID>,
        JpaSpecificationExecutor<Curso> {

    List<Curso> findByDocenteId(UUID docenteId);

    List<Curso> findByEstado(EstadoCurso estado);

    List<Curso> findByNivelCefr(NivelCefr nivelCefr);

    List<Curso> findByDocenteIdAndEstado(UUID docenteId, EstadoCurso estado);

    // Carga el docente en la misma query. Necesario para que CursoMapper.toResponse
    // lea los escalares del docente sin disparar un SELECT adicional
    // (open-in-view=false + docente es @ManyToOne LAZY).
    @EntityGraph(attributePaths = {"docente"})
    @Query("SELECT c FROM Curso c WHERE c.id = :id")
    Optional<Curso> findWithDocenteById(@Param("id") UUID id);

    // Sobrescribe el findAll de JpaSpecificationExecutor para aplicar @EntityGraph.
    // Spring Data JPA 3.x+ respeta @EntityGraph en metodos sobrescritos explicitamente.
    // Para @ManyToOne no hay problema de paginacion: Hibernate hace un LEFT JOIN
    // y pagina sobre la fila principal (cursos), sin duplicar filas.
    @Override
    @EntityGraph(attributePaths = {"docente"})
    Page<Curso> findAll(@Nullable Specification<Curso> spec, Pageable pageable);
}