package com.kellyacademy.enrollment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CrearEntregaRequest(

        @NotNull(message = "El ID de la tarea es obligatorio")
        UUID tareaId,

        @NotNull(message = "El ID del estudiante es obligatorio")
        UUID estudianteId,

        @NotBlank(message = "La URL del archivo es obligatoria")
        @Size(max = 500, message = "La URL del archivo no puede exceder 500 caracteres")
        String urlArchivo
) {
}