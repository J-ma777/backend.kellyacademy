package com.kellyacademy.course.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CrearTareaRequest(

        @NotNull(message = "La semana es obligatoria")
        UUID semanaId,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 200, message = "El titulo no puede exceder 200 caracteres")
        String titulo,

        @Size(max = 5000, message = "La descripcion no puede exceder 5000 caracteres")
        String descripcion,

        @Size(max = 500, message = "La URL de instrucciones no puede exceder 500 caracteres")
        String instruccionesUrl,

        LocalDateTime fechaLimite,

        @NotNull(message = "El puntaje maximo es obligatorio")
        @Min(value = 1, message = "El puntaje maximo debe ser al menos 1")
        @Max(value = 1000, message = "El puntaje maximo no puede exceder 1000")
        Integer puntajeMaximo
) {
}