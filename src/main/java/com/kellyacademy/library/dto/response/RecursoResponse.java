package com.kellyacademy.library.dto.response;

import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.enums.TipoMaterial;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecursoResponse(
        UUID id,
        String titulo,
        String descripcion,
        String categoria,
        NivelCefr nivelCefr,
        TipoMaterial tipo,
        String urlArchivo,
        String urlExterno,
        BigDecimal tamanoMb,
        Integer contadorDescargas,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}