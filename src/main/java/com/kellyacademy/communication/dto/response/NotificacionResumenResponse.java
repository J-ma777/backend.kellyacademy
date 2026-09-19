package com.kellyacademy.communication.dto.response;

import com.kellyacademy.communication.enums.TipoNotificacion;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificacionResumenResponse(
        UUID id,
        TipoNotificacion tipo,
        String titulo,
        String link,
        Boolean leida,
        LocalDateTime fechaCreacion
) {
}