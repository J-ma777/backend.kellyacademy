package com.kellyacademy.security.config;

import com.kellyacademy.shared.config.AppTime;
import com.kellyacademy.shared.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        log.warn(
                "Acceso no autorizado a URI: {} - Razon: {}",
                request.getRequestURI(),
                authException.getMessage()
        );

        ErrorResponse error = ErrorResponse.builder()
                .codigo("UNAUTHORIZED")
                .mensaje("Autenticacion requerida para acceder a este recurso")
                .estado(HttpStatus.UNAUTHORIZED.value())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now(AppTime.ZONA_NEGOCIO))
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), error);
    }
}