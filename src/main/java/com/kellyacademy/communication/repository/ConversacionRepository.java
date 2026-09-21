package com.kellyacademy.communication.repository;

import com.kellyacademy.communication.entity.Conversacion;
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

public interface ConversacionRepository extends
        JpaRepository<Conversacion, UUID>,
        JpaSpecificationExecutor<Conversacion> {

    List<Conversacion> findByParticipante1IdOrParticipante2IdOrderByUltimoMensajeAtDesc(UUID id1, UUID id2);

    Optional<Conversacion> findByCursoIdAndParticipante1IdAndParticipante2Id(UUID cursoId, UUID p1, UUID p2);

    // Conversaciones sin curso (chat directo). findByCursoId... no matchea con NULL en SQL.
    Optional<Conversacion> findByCursoIsNullAndParticipante1IdAndParticipante2Id(UUID p1, UUID p2);

    // Carga curso + participantes en la misma query. Necesario para que
    // ConversacionMapper.toResponse lea escalares sin LazyInitializationException
    // (open-in-view=false + @ManyToOne LAZY).
    @EntityGraph(attributePaths = {
            "curso",
            "participante1",
            "participante2"
    })
    @Query("SELECT c FROM Conversacion c WHERE c.id = :id")
    Optional<Conversacion> findWithParticipantesById(@Param("id") UUID id);

    // Sobrescribe findAll(Specification, Pageable) para aplicar @EntityGraph.
    @Override
    @EntityGraph(attributePaths = {
            "curso",
            "participante1",
            "participante2"
    })
    Page<Conversacion> findAll(@Nullable Specification<Conversacion> spec, Pageable pageable);
}