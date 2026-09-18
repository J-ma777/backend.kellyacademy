package com.kellyacademy.course.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

// Para listados dentro de una semana. Incluye fechaLimite para agenda.
public record TareaResumenResponse(
        UUID id,
        String titulo,
        LocalDateTime fechaLimite,
        Integer puntajeMaximo
) {
}