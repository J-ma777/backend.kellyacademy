package com.kellyacademy.enrollment.dto.request;

import jakarta.validation.constraints.Size;

/*
 Request para actualizar la entrega del estudiante.
 urlArchivo es nullable desde el slice #77: permite al estudiante corregir
 metadatos (ej. borrar la URL antes de resubir) sin resubir archivo. Si viene
 con valor, se valida el formato (URL_INVALIDA). null borra la URL actual
 (semantica de reemplazo, coherente con el resto del proyecto).
 */
public record ActualizarEntregaRequest(

        @Size(max = 500, message = "La URL del archivo no puede exceder 500 caracteres")
        String urlArchivo
) {
}