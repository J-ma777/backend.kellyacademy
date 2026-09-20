package com.kellyacademy.course.repository;

import com.kellyacademy.course.entity.Material;
import com.kellyacademy.course.enums.TipoMaterial;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaterialRepository extends JpaRepository<Material, UUID> {

    List<Material> findBySemanaId(UUID semanaId);

    List<Material> findBySemanaIdAndTipo(UUID semanaId, TipoMaterial tipo);

    boolean existsBySemanaId(UUID semanaId);

    @EntityGraph(attributePaths = {
            "semana",
            "semana.unidad",
            "semana.unidad.curso",
            "semana.unidad.curso.docente"
    })
    @Query("SELECT m FROM Material m WHERE m.id = :id")
    Optional<Material> findWithSemanaCursoDocenteById(@Param("id") UUID id);
}