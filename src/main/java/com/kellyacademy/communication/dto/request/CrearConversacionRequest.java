package com.kellyacademy.communication.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CrearConversacionRequest(

        // Opcional: conversacion directa sin contexto de curso
        UUID cursoId,

        // El servicio resuelve participante1 desde el SecurityContext.
        // Solo se recibe el ID del otro participante para evitar suplantacion.
        @NotNull(message = "El ID del otro participante es obligatorio")
        UUID otroParticipanteId,

        @Size(max = 200, message = "El asunto no puede exceder 200 caracteres")
        String asunto
) {
}