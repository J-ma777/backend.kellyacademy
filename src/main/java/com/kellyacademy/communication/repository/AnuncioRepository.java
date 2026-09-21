package com.kellyacademy.communication.repository;

import com.kellyacademy.communication.entity.Anuncio;
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

public interface AnuncioRepository extends
        JpaRepository<Anuncio, UUID>,
        JpaSpecificationExecutor<Anuncio> {

    List<Anuncio> findByCursoIdAndActivoTrueOrderByFechaCreacionDesc(UUID cursoId);

    List<Anuncio> findByAutorId(UUID autorId);

    // Carga curso (con docente para autorizacion) y autor en la misma query.
    // Necesario para que AnuncioMapper.toResponse lea los escalares sin
    // disparar SELECTs adicionales (open-in-view=false).
    @EntityGraph(attributePaths = {
            "curso",
            "curso.docente",
            "autor"
    })
    @Query("SELECT a FROM Anuncio a WHERE a.id = :id")
    Optional<Anuncio> findWithCursoAndAutorById(@Param("id") UUID id);

    // Sobrescribe findAll(Specification, Pageable) para aplicar @EntityGraph.
    @Override
    @EntityGraph(attributePaths = {
            "curso",
            "curso.docente",
            "autor"
    })
    Page<Anuncio> findAll(@Nullable Specification<Anuncio> spec, Pageable pageable);
}