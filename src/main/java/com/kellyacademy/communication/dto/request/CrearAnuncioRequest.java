package com.kellyacademy.communication.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CrearAnuncioRequest(

        @NotNull(message = "El ID del curso es obligatorio")
        UUID cursoId,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 200, message = "El titulo no puede exceder 200 caracteres")
        String titulo,

        @NotBlank(message = "El cuerpo es obligatorio")
        @Size(max = 50000) String cuerpo,
        Boolean publicado,
        Boolean fijado,
        @Size(max=500) String imagenUrl
) {
    public CrearAnuncioRequest(UUID cursoId, String titulo, String cuerpo) { this(cursoId, titulo, cuerpo, null, null, null); }
    public CrearAnuncioRequest(UUID cursoId,String titulo,String cuerpo,Boolean publicado,Boolean fijado){this(cursoId,titulo,cuerpo,publicado,fijado,null);}
}