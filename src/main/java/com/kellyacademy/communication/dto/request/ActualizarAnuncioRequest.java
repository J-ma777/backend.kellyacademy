package com.kellyacademy.communication.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarAnuncioRequest(

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 200, message = "El titulo no puede exceder 200 caracteres")
        String titulo,

        @NotBlank(message = "El cuerpo es obligatorio")
        @Size(max = 50000) String cuerpo,
        Boolean publicado,
        Boolean fijado,
        @Size(max=500) String imagenUrl
) {
    public ActualizarAnuncioRequest(String titulo, String cuerpo) { this(titulo, cuerpo, null, null, null); }
    public ActualizarAnuncioRequest(String titulo,String cuerpo,Boolean publicado,Boolean fijado){this(titulo,cuerpo,publicado,fijado,null);}
}