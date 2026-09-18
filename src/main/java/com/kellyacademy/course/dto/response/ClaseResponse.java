package com.kellyacademy.course.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClaseResponse(
        UUID id,
        UUID semanaId,
        String titulo,
        String descripcion,
        String urlVivo,
        String urlGrabacion,
        LocalDateTime fechaHora,
        Integer duracionMinutos,
        String sala,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}