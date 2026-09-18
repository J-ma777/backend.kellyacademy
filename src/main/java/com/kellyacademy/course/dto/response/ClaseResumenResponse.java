package com.kellyacademy.course.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

// Para listados dentro de una semana. Incluye solo lo esencial de agenda.
public record ClaseResumenResponse(
        UUID id,
        String titulo,
        LocalDateTime fechaHora,
        Integer duracionMinutos,
        String sala
) {
}