package com.kellyacademy.course.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CrearUnidadRequest(

        @NotNull(message = "El curso es obligatorio")
        UUID cursoId,

        @NotNull(message = "El numero es obligatorio")
        @Min(value = 1, message = "El numero debe ser al menos 1")
        Integer numero,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 200, message = "El titulo no puede exceder 200 caracteres")
        String titulo,

        @Size(max = 5000, message = "La descripcion no puede exceder 5000 caracteres")
        String descripcion
) {
}