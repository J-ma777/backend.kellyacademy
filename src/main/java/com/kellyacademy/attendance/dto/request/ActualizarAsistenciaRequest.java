package com.kellyacademy.attendance.dto.request;

import com.kellyacademy.attendance.enums.EstadoAsistencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarAsistenciaRequest(

        // 'estado' es dato operativo editable, no campo administrativo.
        // A diferencia de EstadoUsuario o EstadoCurso, cambiarlo es la operacion normal.
        @NotNull(message = "El estado de asistencia es obligatorio")
        EstadoAsistencia estado,

        @Size(max = 500, message = "La observacion no puede exceder 500 caracteres")
        String observacion
) {
}