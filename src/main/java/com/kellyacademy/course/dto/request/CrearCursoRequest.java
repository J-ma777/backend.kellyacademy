package com.kellyacademy.course.dto.request;

import com.kellyacademy.course.enums.NivelCefr;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CrearCursoRequest(

        @NotNull(message = "El docente es obligatorio")
        UUID docenteId,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 200, message = "El titulo no puede exceder 200 caracteres")
        String titulo,

        @Size(max = 5000, message = "La descripcion no puede exceder 5000 caracteres")
        String descripcion,

        @NotNull(message = "El nivel CEFR es obligatorio")
        NivelCefr nivelCefr,

        @Size(max = 200, message = "El horario no puede exceder 200 caracteres")
        String horario,

        LocalDate fechaInicio,

        LocalDate fechaFin,

        @NotNull(message = "La capacidad maxima es obligatoria")
        @Min(value = 1, message = "La capacidad maxima debe ser al menos 1")
        Integer capacidadMaxima
) {
}