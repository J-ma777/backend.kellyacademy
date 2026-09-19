package com.kellyacademy.communication.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record MensajeResponse(
        UUID id,
        UUID conversacionId,
        UUID remitenteId,
        String remitenteNombreCompleto,
        String cuerpo,
        String adjuntoUrl,
        Boolean leido,
        LocalDateTime enviadoAt,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}