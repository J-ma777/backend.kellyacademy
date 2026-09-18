package com.kellyacademy.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

// Request para crear un usuario.
// La contraseña se recibe en claro (HTTPS obligatorio) y se hashea en el servicio.

public record CrearUsuarioRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
        String apellido,

        @NotBlank(message = "El correo electronico es obligatorio")
        @Email(message = "El correo electronico no tiene un formato valido")
        @Size(max = 255, message = "El correo electronico no puede exceder 255 caracteres")
        String correoElectronico,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "La contraseña debe contener al menos una letra y un numero"
        )
        String contrasena,

        @Size(max = 500, message = "La URL del avatar no puede exceder 500 caracteres")
        String avatarUrl,

        @NotEmpty(message = "El usuario debe tener al menos un rol")
        Set<String> roles
) {
}
