package com.kellyacademy.calendar.dto.request;

import com.kellyacademy.calendar.enums.EstadoTutoria;
import jakarta.validation.constraints.NotNull;

/*
 Request para cambiar el estado de una tutoria.
 Endpoint dedicado: PATCH /api/tutorias/{id}/estado.
 El estado no va en CrearTutoriaRequest (se inicializa PENDIENTE) ni en
 ActualizarTutoriaRequest (va por este PATCH dedicado con maquina de estados).
 */
public record CambiarEstadoTutoriaRequest(

        @NotNull(message = "El estado es obligatorio")
        EstadoTutoria estado
) {
}