package com.kellyacademy.course.dto.request;

import com.kellyacademy.course.enums.EstadoCurso;
import jakarta.validation.constraints.NotNull;

/*
 Request administrativo para cambiar el estado de un curso.
 Endpoint dedicado: PATCH /api/cursos/{id}/estado.
 El estado no va en ActualizarCursoRequest: es campo administrativo con maquina de estados.
 */
public record CambiarEstadoCursoRequest(

        @NotNull(message = "El estado es obligatorio")
        EstadoCurso estado
) {
}