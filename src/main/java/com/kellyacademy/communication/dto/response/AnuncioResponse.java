package com.kellyacademy.communication.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnuncioResponse(
        UUID id,
        UUID cursoId,
        String cursoTitulo,
        UUID autorId,
        String autorNombreCompleto,
        String titulo,
        String cuerpo,
        Boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}