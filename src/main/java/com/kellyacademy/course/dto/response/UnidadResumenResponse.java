package com.kellyacademy.course.dto.response;

import java.util.UUID;

// Para listados dentro de un curso.
public record UnidadResumenResponse(
        UUID id,
        Integer numero,
        String titulo
) {
}