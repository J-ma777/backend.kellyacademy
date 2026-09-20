package com.kellyacademy.security.jwt;

import com.kellyacademy.security.user.CustomUserDetailsService;
import com.kellyacademy.shared.config.AppTime;
import com.kellyacademy.shared.exception.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            String jwtToken = authorizationHeader.substring(7);

            String username = jwtService.extraerNombreUsuario(jwtToken);

            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = customUserDetailsService
                        .loadUserByUsername(username);

                if (jwtService.esTokenValido(jwtToken, userDetails)) {

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authenticationToken);

                    log.debug("Usuario autenticado mediante JWT: {}", username);
                }
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {

            log.warn("JWT expirado para URI: {}", request.getRequestURI());

            escribirError(
                    response,
                    request,
                    "TOKEN_EXPIRED",
                    "El token ha expirado. Por favor, inicia sesión nuevamente.",
                    HttpStatus.UNAUTHORIZED
            );

        } catch (JwtException ex) {

            log.warn("JWT invalido para URI: {}", request.getRequestURI());

            escribirError(
                    response,
                    request,
                    "INVALID_TOKEN",
                    "El token proporcionado no es valido.",
                    HttpStatus.UNAUTHORIZED
            );

        } catch (Exception ex) {

            log.error("Error procesando autenticacion JWT", ex);

            escribirError(
                    response,
                    request,
                    "AUTHENTICATION_ERROR",
                    "Error procesando la autenticacion.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private void escribirError(
            HttpServletResponse response,
            HttpServletRequest request,
            String codigo,
            String mensaje,
            HttpStatus status
    ) throws IOException {

        ErrorResponse error = ErrorResponse.builder()
                .codigo(codigo)
                .mensaje(mensaje)
                .estado(status.value())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now(AppTime.ZONA_NEGOCIO))
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), error);
    }
}