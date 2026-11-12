package com.kellyacademy.security.terms;

import com.kellyacademy.security.user.CustomUserDetails;
import com.kellyacademy.shared.config.AppTime;
import com.kellyacademy.shared.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

/** Se instancia solo en la cadena Spring Security, después de autenticar el JWT. */
@RequiredArgsConstructor
public class TerminosFilter extends OncePerRequestFilter {
    private final TerminosService terminos;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean exento = "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.equals("/auth/login") || path.equals("/api/terminos/aceptacion")
                || path.equals("/actuator/health") || path.startsWith("/swagger-ui/")
                || path.equals("/swagger-ui.html") || path.startsWith("/v3/api-docs") || path.equals("/error");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!exento && auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails principal
                && !terminos.estado(principal.getUsuario().getId(), auth).aceptado()) {
            var error = ErrorResponse.builder().codigo("TERMINOS_PENDIENTES")
                    .mensaje("Debes revisar y aceptar los términos y condiciones para continuar.")
                    .estado(HttpStatus.FORBIDDEN.value()).ruta(request.getRequestURI())
                    .timestamp(LocalDateTime.now(AppTime.ZONA_NEGOCIO)).build();
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-store");
            objectMapper.writeValue(response.getWriter(), error);
            return;
        }
        chain.doFilter(request, response);
    }
}
