package com.kellyacademy.security.auth.service;

import com.kellyacademy.user.entity.Permiso;
import com.kellyacademy.user.entity.Rol;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.security.auth.dto.AuthRequest;
import com.kellyacademy.security.auth.dto.AuthResponse;
import com.kellyacademy.security.jwt.JwtService;
import com.kellyacademy.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse login(AuthRequest request) {

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getCorreoElectronico(),
                        request.getContrasena()
                )
        );

        CustomUserDetails customUserDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Usuario usuario =
                customUserDetails.getUsuario();

        String token =
                jwtService.generarToken(customUserDetails);

        Set<String> roles = usuario.getRoles()
                .stream()
                .map(Rol::getNombre)
                .collect(Collectors.toSet());

        Set<String> permisos = usuario.getRoles()
                .stream()
                .flatMap(rol -> rol.getPermisos().stream())
                .map(Permiso::getNombre)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(usuario.getId())
                .correoElectronico(usuario.getCorreoElectronico())
                .roles(roles)
                .permisos(permisos)
                .build();
    }
}