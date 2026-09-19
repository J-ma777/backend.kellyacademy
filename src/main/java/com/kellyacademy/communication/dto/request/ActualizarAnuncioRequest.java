package com.kellyacademy.communication.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarAnuncioRequest(

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 200, message = "El titulo no puede exceder 200 caracteres")
        String titulo,

        @NotBlank(message = "El cuerpo es obligatorio")
        String cuerpo
) {
}