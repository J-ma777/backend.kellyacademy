package com.kellyacademy.enrollment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarEntregaRequest(

        @NotBlank(message = "La URL del archivo es obligatoria")
        @Size(max = 500, message = "La URL del archivo no puede exceder 500 caracteres")
        String urlArchivo
) {
}