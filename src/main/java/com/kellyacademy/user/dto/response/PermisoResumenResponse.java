package com.kellyacademy.user.dto.response;

import java.util.UUID;

// Para listados: sin descripcion.
public record PermisoResumenResponse(
        UUID id,
        String nombre
) {
}