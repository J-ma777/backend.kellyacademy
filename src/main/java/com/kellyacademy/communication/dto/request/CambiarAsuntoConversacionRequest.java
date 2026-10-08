package com.kellyacademy.communication.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 Request para cambiar el asunto de una conversacion.
 Endpoint dedicado: PATCH /api/conversaciones/{id}/asunto.
 El asunto no se pone a null por este endpoint: si el producto quiere
 "quitar asunto", va con otro codigo explicito (no ambiguo).
 */
public record CambiarAsuntoConversacionRequest(

        @NotBlank(message = "El asunto es obligatorio")
        @Size(max = 200, message = "El asunto no puede exceder 200 caracteres")
        String asunto
) {
}