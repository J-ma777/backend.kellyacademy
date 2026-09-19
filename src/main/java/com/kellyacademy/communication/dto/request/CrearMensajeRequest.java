package com.kellyacademy.communication.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearMensajeRequest(

        @NotBlank(message = "El cuerpo del mensaje es obligatorio")
        String cuerpo,

        @Size(max = 500, message = "La URL del adjunto no puede exceder 500 caracteres")
        String adjuntoUrl
) {
}