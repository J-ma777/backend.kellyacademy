package com.kellyacademy.communication.dto.response;

import com.kellyacademy.communication.enums.TipoNotificacion;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificacionResponse(
        UUID id,
        UUID usuarioId,
        TipoNotificacion tipo,
        String titulo,
        String cuerpo,
        String link,
        Boolean leida,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}