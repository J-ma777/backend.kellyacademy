package com.kellyacademy.course.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UnidadResponse(
        UUID id,
        UUID cursoId,
        Integer numero,
        String titulo,
        String descripcion,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}