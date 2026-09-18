package com.kellyacademy.user.dto.response;

import com.kellyacademy.user.enums.EstadoUsuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


//DTO de respuesta con la informacion completa de un usuario.
//NUNCA expone contrasena ni la jerarquia de permisos.

public record UsuarioResponse(
        UUID id,
        String nombre,
        String apellido,
        String correoElectronico,
        String avatarUrl,
        EstadoUsuario estado,
        List<String> roles,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}