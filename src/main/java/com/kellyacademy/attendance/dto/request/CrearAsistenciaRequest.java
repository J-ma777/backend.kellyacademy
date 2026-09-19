package com.kellyacademy.attendance.dto.request;

import com.kellyacademy.attendance.enums.EstadoAsistencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CrearAsistenciaRequest(

        @NotNull(message = "El ID de la clase es obligatorio")
        UUID claseId,

        @NotNull(message = "El ID del estudiante es obligatorio")
        UUID estudianteId,

        @NotNull(message = "El estado de asistencia es obligatorio")
        EstadoAsistencia estado,

        @Size(max = 500, message = "La observacion no puede exceder 500 caracteres")
        String observacion
) {
}