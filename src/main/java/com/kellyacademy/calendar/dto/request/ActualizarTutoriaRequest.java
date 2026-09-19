package com.kellyacademy.calendar.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record ActualizarTutoriaRequest(

        @NotNull(message = "La fecha es obligatoria")
        LocalDateTime fecha,

        @NotNull(message = "La duracion es obligatoria")
        @Positive(message = "La duracion debe ser positiva")
        @Min(value = 15, message = "La duracion minima es 15 minutos")
        @Max(value = 240, message = "La duracion maxima es 240 minutos")
        Integer duracionMinutos,

        String notas
) {
}