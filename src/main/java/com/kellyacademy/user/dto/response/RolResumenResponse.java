package com.kellyacademy.user.dto.response;

import java.util.UUID;

// Para listados y para anidar en UsuarioResponse si hiciera falta.
public record RolResumenResponse(
        UUID id,
        String nombre,
        String descripcion
) {
}