package com.kellyacademy.library.dto.response;

import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.enums.TipoMaterial;

import java.util.UUID;

public record RecursoResumenResponse(
        UUID id,
        String titulo,
        String categoria,
        NivelCefr nivelCefr,
        TipoMaterial tipo,
        Integer contadorDescargas
) {
}