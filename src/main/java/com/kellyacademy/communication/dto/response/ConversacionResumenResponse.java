package com.kellyacademy.communication.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversacionResumenResponse(
        UUID id,
        UUID cursoId,
        String cursoTitulo,
        UUID participante1Id,
        String participante1NombreCompleto,
        UUID participante2Id,
        String participante2NombreCompleto,
        String asunto,
        LocalDateTime ultimoMensajeAt,
        Long mensajesNoLeidos
) {
}