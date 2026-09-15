package com.kellyacademy.communication.repository;

import com.kellyacademy.communication.entity.Notificacion;
import com.kellyacademy.communication.enums.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(UUID usuarioId);

    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(UUID usuarioId);

    long countByUsuarioIdAndLeidaFalse(UUID usuarioId);

    List<Notificacion> findByUsuarioIdAndTipo(UUID usuarioId, TipoNotificacion tipo);
}