package com.kellyacademy.course.dto.response;

import java.util.UUID;

// Para listados dentro de una unidad.
public record SemanaResumenResponse(
        UUID id,
        Integer numero,
        String titulo,
        Boolean esActual
) {
}