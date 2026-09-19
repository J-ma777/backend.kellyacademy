package com.kellyacademy.calendar.dto.response;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record DisponibilidadResumenResponse(
        UUID id,
        UUID docenteId,
        String docenteNombreCompleto,
        DayOfWeek diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        Boolean bloqueada
) {
}