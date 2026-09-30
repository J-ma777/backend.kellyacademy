package com.kellyacademy.enrollment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/*
 Request para calificar una entrega.
 Endpoint dedicado: PATCH /api/entregas/{id}/calificar.
 El rango maximo (puntajeMaximo de la Tarea) se valida en el servicio:
 es un limite dinamico, no expresable con anotaciones estaticas.
 */
public record CalificarEntregaRequest(

        @NotNull(message = "La nota es obligatoria")
        @DecimalMin(value = "0.00", message = "La nota no puede ser negativa")
        BigDecimal nota,

        @Size(max = 2000, message = "La retroalimentacion no puede exceder 2000 caracteres")
        String retroalimentacion
) {
}