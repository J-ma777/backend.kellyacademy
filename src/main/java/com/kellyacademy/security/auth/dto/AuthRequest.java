package com.kellyacademy.security.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {
    @NotBlank
    @Email
    private String correoElectronico;

    @NotBlank
    private String contrasena;
}
