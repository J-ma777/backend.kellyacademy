package com.kellyacademy.communication.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnuncioResumenResponse(
        UUID id,
        UUID cursoId,
        String cursoTitulo,
        UUID autorId,
        String autorNombreCompleto,
        String titulo,
        Boolean activo,
        LocalDateTime fechaCreacion
) {
}