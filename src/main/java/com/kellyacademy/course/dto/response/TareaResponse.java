package com.kellyacademy.course.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record TareaResponse(
        UUID id,
        UUID semanaId,
        String titulo,
        String descripcion,
        String instruccionesUrl,
        LocalDateTime fechaLimite,
        Integer puntajeMaximo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}