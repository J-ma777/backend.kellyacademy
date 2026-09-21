package com.kellyacademy.calendar.repository;

import com.kellyacademy.calendar.entity.Evento;
import com.kellyacademy.calendar.enums.TipoEvento;
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

public interface EventoRepository extends
        JpaRepository<Evento, UUID>,
        JpaSpecificationExecutor<Evento> {

    List<Evento> findByUsuarioIdOrderByInicioAsc(UUID usuarioId);

    List<Evento> findByUsuarioIdAndInicioBetween(UUID usuarioId, LocalDateTime inicio, LocalDateTime fin);

    List<Evento> findByCursoId(UUID cursoId);

    List<Evento> findByUsuarioIdAndTipo(UUID usuarioId, TipoEvento tipo);

    // Carga usuario y curso en la misma query. Necesario para que EventoMapper
    // lea los escalares sin disparar SELECTs adicionales
    // (open-in-view=false + @ManyToOne LAZY).
    @EntityGraph(attributePaths = {"usuario", "curso"})
    @Query("SELECT e FROM Evento e WHERE e.id = :id")
    Optional<Evento> findWithUsuarioAndCursoById(@Param("id") UUID id);

    // Sobrescribe el findAll de JpaSpecificationExecutor para aplicar @EntityGraph.
    // Mismo patron que CursoRepository y MatriculaRepository.
    @Override
    @EntityGraph(attributePaths = {"usuario", "curso"})
    Page<Evento> findAll(@Nullable Specification<Evento> spec, Pageable pageable);
}
