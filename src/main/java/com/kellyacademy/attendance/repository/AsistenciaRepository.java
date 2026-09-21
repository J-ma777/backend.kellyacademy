package com.kellyacademy.attendance.repository;

import com.kellyacademy.attendance.entity.Asistencia;
import com.kellyacademy.attendance.enums.EstadoAsistencia;
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

public interface AsistenciaRepository extends
        JpaRepository<Asistencia, UUID>,
        JpaSpecificationExecutor<Asistencia> {

    List<Asistencia> findByClaseId(UUID claseId);

    List<Asistencia> findByEstudianteId(UUID estudianteId);

    Optional<Asistencia> findByClaseIdAndEstudianteId(UUID claseId, UUID estudianteId);

    List<Asistencia> findByClaseIdAndEstado(UUID claseId, EstadoAsistencia estado);

    long countByEstudianteIdAndEstado(UUID estudianteId, EstadoAsistencia estado);

    long countByEstudianteId(UUID estudianteId);

    // Carga clase (con su jerarquia hasta docente) y estudiante en la misma query.
    // Necesario para que AsistenciaMapper.toResponse lea los escalares sin disparar
    // SELECTs adicionales (open-in-view=false + @ManyToOne LAZY).
    // Se carga toda la jerarquia de Clase porque la autorizacion de docente dueno
    // navega clase.semana.unidad.curso.docente.id.
    @EntityGraph(attributePaths = {
            "clase",
            "clase.semana",
            "clase.semana.unidad",
            "clase.semana.unidad.curso",
            "clase.semana.unidad.curso.docente",
            "estudiante"
    })
    @Query("SELECT a FROM Asistencia a WHERE a.id = :id")
    Optional<Asistencia> findWithClaseAndEstudianteById(@Param("id") UUID id);

    // Sobrescribe el findAll de JpaSpecificationExecutor para aplicar @EntityGraph.
    // Mismo patron que MatriculaRepository y CursoRepository.
    @Override
    @EntityGraph(attributePaths = {
            "clase",
            "clase.semana",
            "clase.semana.unidad",
            "clase.semana.unidad.curso",
            "clase.semana.unidad.curso.docente",
            "estudiante"
    })
    Page<Asistencia> findAll(@Nullable Specification<Asistencia> spec, Pageable pageable);
}