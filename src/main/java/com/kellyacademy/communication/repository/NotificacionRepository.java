package com.kellyacademy.communication.repository;

import com.kellyacademy.communication.entity.Notificacion;
import com.kellyacademy.communication.enums.TipoNotificacion;
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

public interface NotificacionRepository extends
        JpaRepository<Notificacion, UUID>,
        JpaSpecificationExecutor<Notificacion> {

    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(UUID usuarioId);

    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(UUID usuarioId);

    long countByUsuarioIdAndLeidaFalse(UUID usuarioId);

    List<Notificacion> findByUsuarioIdAndTipo(UUID usuarioId, TipoNotificacion tipo);

    // Carga el usuario en la misma query. Necesario para que NotificacionMapper
    // lea el id sin disparar SELECT adicional (open-in-view=false).
    @EntityGraph(attributePaths = {"usuario"})
    @Query("SELECT n FROM Notificacion n WHERE n.id = :id")
    Optional<Notificacion> findWithUsuarioById(@Param("id") UUID id);

    // Marca todas las no leidas del usuario como leidas en una sola sentencia.
    // Bulk update: no carga entidades en memoria. Devuelve filas afectadas.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario.id = :usuarioId AND n.leida = false")
    int marcarTodasLeidasPorUsuario(@Param("usuarioId") UUID usuarioId);

    // Sobrescribe findAll(Specification, Pageable) para aplicar @EntityGraph.
    // Mismo patron que MatriculaRepository y AsistenciaRepository.
    @Override
    @EntityGraph(attributePaths = {"usuario"})
    Page<Notificacion> findAll(@Nullable Specification<Notificacion> spec, Pageable pageable);
}