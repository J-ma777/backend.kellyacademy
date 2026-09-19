package com.kellyacademy.library.dto.request;

import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.enums.TipoMaterial;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CrearRecursoRequest(

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 200, message = "El titulo no puede exceder 200 caracteres")
        String titulo,

        String descripcion,

        @Size(max = 100, message = "La categoria no puede exceder 100 caracteres")
        String categoria,

        NivelCefr nivelCefr,

        @NotNull(message = "El tipo de material es obligatorio")
        TipoMaterial tipo,

        @Size(max = 500, message = "La URL del archivo no puede exceder 500 caracteres")
        String urlArchivo,

        @Size(max = 500, message = "La URL externa no puede exceder 500 caracteres")
        String urlExterno,

        @PositiveOrZero(message = "El tamano no puede ser negativo")
        BigDecimal tamanoMb
) {
}