package com.kellyacademy.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


/* Request para actualizar un usuario existente.
  NO incluye contrasena (se cambia con endpoint dedicado /users/{id}/password).
  NO incluye correo (cambiarlo requiere verificacion adicional).
  NO incluye estado (lo gestiona un endpoint administrativo).
  NO incluye roles (se gestionan con endpoints dedicados).
 */
public record ActualizarUsuarioRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
        String apellido,

        @Size(max = 500, message = "La URL del avatar no puede exceder 500 caracteres")
        String avatarUrl
) {
}