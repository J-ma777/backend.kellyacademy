package com.kellyacademy.course.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SemanaResponse(
        UUID id,
        UUID unidadId,
        Integer numero,
        String titulo,
        String descripcion,
        Boolean esActual,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}