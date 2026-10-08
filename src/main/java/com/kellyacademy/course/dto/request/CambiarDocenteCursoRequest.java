package com.kellyacademy.course.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CambiarDocenteCursoRequest(
        @NotNull(message = "El ID del docente es obligatorio")
        UUID docenteId
) {
}
