package com.kellyacademy.communication.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record MensajeResumenResponse(
        UUID id,
        UUID conversacionId,
        UUID remitenteId,
        String remitenteNombreCompleto,
        String cuerpo,
        Boolean leido,
        LocalDateTime enviadoAt
) {
}