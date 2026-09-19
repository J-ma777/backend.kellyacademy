package com.kellyacademy.calendar.dto.request;

import com.kellyacademy.calendar.enums.TipoEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CrearEventoRequest(

        // Opcional: vincula el evento a un curso. Inmutable tras creacion.
        UUID cursoId,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 200, message = "El titulo no puede exceder 200 caracteres")
        String titulo,

        String descripcion,

        @NotNull(message = "El tipo de evento es obligatorio")
        TipoEvento tipo,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDateTime inicio,

        LocalDateTime fin,

        @Size(max = 100, message = "La sala no puede exceder 100 caracteres")
        String sala
) {
}