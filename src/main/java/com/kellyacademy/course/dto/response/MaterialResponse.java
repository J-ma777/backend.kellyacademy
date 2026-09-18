package com.kellyacademy.course.dto.response;

import com.kellyacademy.course.enums.TipoMaterial;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MaterialResponse(
        UUID id,
        UUID semanaId,
        String titulo,
        String descripcion,
        String urlArchivo,
        TipoMaterial tipo,
        BigDecimal tamanoMb,
        String urlExterno,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}