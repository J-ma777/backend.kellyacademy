package com.kellyacademy.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CrearMatriculaRequest(

        @NotNull(message = "El ID del curso es obligatorio")
        UUID cursoId,

        @NotNull(message = "El ID del estudiante es obligatorio")
        UUID estudianteId
) {
}