package com.kellyacademy.course.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.UUID;

public record CrearClaseRequest(

        @NotNull(message = "La semana es obligatoria")
        UUID semanaId,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 200, message = "El titulo no puede exceder 200 caracteres")
        String titulo,

        @Size(max = 5000, message = "La descripcion no puede exceder 5000 caracteres")
        String descripcion,

        @Size(max = 500, message = "La URL en vivo no puede exceder 500 caracteres")
        String urlVivo,

        @Size(max = 500, message = "La URL de grabacion no puede exceder 500 caracteres")
        String urlGrabacion,

        LocalDateTime fechaHora,

        @Min(value = 1, message = "La duración debe ser al menos 1 minuto")
        @Max(value = 600, message = "La duracion no puede exceder 600 minutos")
        Integer duracionMinutos,

        @Size(max = 100, message = "La sala no puede exceder 100 caracteres")
        String sala
) {
}