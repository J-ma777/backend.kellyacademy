package com.kellyacademy.enrollment.repository;

import com.kellyacademy.enrollment.entity.Entrega;
import com.kellyacademy.enrollment.enums.EstadoEntrega;
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

public interface EntregaRepository extends
        JpaRepository<Entrega, UUID>,
        JpaSpecificationExecutor<Entrega> {

    List<Entrega> findByTareaId(UUID tareaId);

    List<Entrega> findByEstudianteId(UUID estudianteId);

    Optional<Entrega> findByTareaIdAndEstudianteId(UUID tareaId, UUID estudianteId);

    List<Entrega> findByTareaIdAndEstado(UUID tareaId, EstadoEntrega estado);

    // Carga la tarea con su jerarquia completa (semana -> unidad -> curso -> docente)
    // y el estudiante. Necesario para que EntregaMapper.toResponse lea los escalares
    // y para validar autorizacion (docente dueno del curso) sin LazyInit.
    // open-in-view=false + @ManyToOne LAZY obliga a este @EntityGraph explicito.
    @EntityGraph(attributePaths = {
            "tarea",
            "tarea.semana",
            "tarea.semana.unidad",
            "tarea.semana.unidad.curso",
            "tarea.semana.unidad.curso.docente",
            "estudiante"
    })
    @Query("SELECT e FROM Entrega e WHERE e.id = :id")
    Optional<Entrega> findWithTareaAndEstudianteById(@Param("id") UUID id);

    // Sobrescribe el findAll de JpaSpecificationExecutor para aplicar @EntityGraph.
    // Mismo patron que CursoRepository y MatriculaRepository.
    @Override
    @EntityGraph(attributePaths = {"tarea", "estudiante"})
    Page<Entrega> findAll(@Nullable Specification<Entrega> spec, Pageable pageable);
}