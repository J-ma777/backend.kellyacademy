package com.kellyacademy.course.dto.response;

import com.kellyacademy.course.enums.TipoMaterial;

import java.math.BigDecimal;
import java.util.UUID;

// Para listados dentro de una semana.
public record MaterialResumenResponse(
        UUID id,
        String titulo,
        TipoMaterial tipo,
        BigDecimal tamanoMb
) {
}