package com.kellyacademy.calendar.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record CrearDisponibilidadRequest(

        @NotNull(message = "El ID del docente es obligatorio")
        UUID docenteId,

        @NotNull(message = "El dia de la semana es obligatorio")
        DayOfWeek diaSemana,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin
) {
}