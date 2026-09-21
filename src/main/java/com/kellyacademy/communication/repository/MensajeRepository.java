package com.kellyacademy.communication.repository;

import com.kellyacademy.communication.entity.Mensaje;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MensajeRepository extends
        JpaRepository<Mensaje, UUID>,
        JpaSpecificationExecutor<Mensaje> {

    List<Mensaje> findByConversacionIdOrderByEnviadoAtAsc(UUID conversacionId);

    long countByConversacionIdAndLeidoFalse(UUID conversacionId);

    long countByConversacionIdAndRemitenteIdNotAndLeidoFalse(UUID conversacionId, UUID remitenteId);

    // Carga conversacion + remitente en la misma query. Necesario para que
    // MensajeMapper.toResponse lea escalares sin LazyInitializationException.
    @EntityGraph(attributePaths = {
            "conversacion",
            "remitente"
    })
    @Query("SELECT m FROM Mensaje m WHERE m.id = :id")
    Optional<Mensaje> findWithConversacionAndRemitenteById(@Param("id") UUID id);

    // Marca todos los mensajes no leidos de la conversacion donde el usuario
    // NO es remitente (i.e., mensajes que le llegaron). Bulk update.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Mensaje m SET m.leido = true "
            + "WHERE m.conversacion.id = :conversacionId "
            + "AND m.remitente.id <> :usuarioId "
            + "AND m.leido = false")
    int marcarTodosLeidosPorUsuario(
            @Param("conversacionId") UUID conversacionId,
            @Param("usuarioId") UUID usuarioId
    );

    // Sobrescribe findAll(Specification, Pageable) para aplicar @EntityGraph.
    @Override
    @EntityGraph(attributePaths = {
            "conversacion",
            "remitente"
    })
    Page<Mensaje> findAll(@Nullable Specification<Mensaje> spec, Pageable pageable);
}