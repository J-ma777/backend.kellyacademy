package com.kellyacademy.course.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CambiarSemanaRequest(
        @NotNull(message = "El ID de la semana es obligatorio")
        UUID semanaId
) {
}
