package com.kellyacademy.calendar.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ActualizarDisponibilidadRequest(

        @NotNull(message = "El dia de la semana es obligatorio")
        DayOfWeek diaSemana,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        // 'bloqueada' es flag operativo del docente (toggle disponible/no disponible).
        // No hay maquina de estados ni transiciones invalidas.
        @NotNull(message = "El estado de bloqueo es obligatorio")
        Boolean bloqueada
) {
}