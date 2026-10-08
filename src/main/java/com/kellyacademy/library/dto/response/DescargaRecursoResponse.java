package com.kellyacademy.library.dto.response;

import java.util.UUID;

/*
 Respuesta del endpoint de descarga.
 Expone solo lo necesario para que el cliente abra el recurso y muestre el
 contador actualizado. No reusa RecursoResponse: evitar mandar descripcion,
 categoria, tamanoMb, etc., que el cliente ya tiene del GET previo.
 */
public record DescargaRecursoResponse(
        UUID id,
        String urlArchivo,
        String urlExterno,
        Integer contadorDescargas
) {
}