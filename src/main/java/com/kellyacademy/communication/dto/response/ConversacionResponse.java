package com.kellyacademy.communication.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversacionResponse(
        UUID id,
        UUID cursoId,
        String cursoTitulo,
        UUID participante1Id,
        String participante1NombreCompleto,
        UUID participante2Id,
        String participante2NombreCompleto,
        String asunto,
        LocalDateTime ultimoMensajeAt,
        // Nullable: null indica "no calculado". El servicio lo setea cuando aplica.
        Long mensajesNoLeidos,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}