package com.kellyacademy.security.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class AuthResponse {

    private final String accessToken;

    private final String tokenType;

    private final UUID userId;

    private final String correoElectronico;

    private final Set<String> roles;

    private final Set<String> permisos;
}