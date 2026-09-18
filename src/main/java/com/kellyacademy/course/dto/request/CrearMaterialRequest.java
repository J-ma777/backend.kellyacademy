package com.kellyacademy.course.dto.request;

import com.kellyacademy.course.enums.TipoMaterial;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CrearMaterialRequest(

        @NotNull(message = "La semana es obligatoria")
        UUID semanaId,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 200, message = "El titulo no puede exceder 200 caracteres")
        String titulo,

        @Size(max = 5000, message = "La descripcion no puede exceder 5000 caracteres")
        String descripcion,

        @Size(max = 500, message = "La URL del archivo no puede exceder 500 caracteres")
        String urlArchivo,

        @NotNull(message = "El tipo de material es obligatorio")
        TipoMaterial tipo,

        @DecimalMin(value = "0.0", message = "El tamano no puede ser negativo")
        BigDecimal tamanoMb,

        @Size(max = 500, message = "La URL externa no puede exceder 500 caracteres")
        String urlExterno
) {
}