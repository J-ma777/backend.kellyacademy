package com.kellyacademy.attendance.dto.request;

import com.kellyacademy.attendance.enums.EstadoAsistencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/*
 Item individual del registro masivo de asistencia.
 El claseId va en el DTO contenedor (CrearAsistenciasMasivasRequest),
 no en cada item: todos los registros del batch son de la misma clase.
 */
public record RegistroAsistenciaItemRequest(

        @NotNull(message = "El ID del estudiante es obligatorio")
        UUID estudianteId,

        @NotNull(message = "El estado de asistencia es obligatorio")
        EstadoAsistencia estado,

        @Size(max = 500, message = "La observacion no puede exceder 500 caracteres")
        String observacion
) {
}