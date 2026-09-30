package com.kellyacademy.enrollment.dto.request;

import com.kellyacademy.enrollment.enums.EstadoMatricula;
import jakarta.validation.constraints.NotNull;

/*
 Request administrativo para cambiar el estado de una matricula.
 Endpoint dedicado: PATCH /api/matriculas/{id}/estado.
 El estado no va en un PUT general: es campo administrativo con maquina de estados.
 */
public record CambiarEstadoMatriculaRequest(

        @NotNull(message = "El estado es obligatorio")
        EstadoMatricula estado
) {
}