package com.kellyacademy.security.jwt;

import com.kellyacademy.security.user.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        try {

            String jwtToken =
                    authorizationHeader.substring(7);

            String username =
                    jwtService.extraerNombreUsuario(jwtToken);

            if (username != null
                    && SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                UserDetails userDetails =
                        customUserDetailsService
                                .loadUserByUsername(username);

                if (jwtService.esTokenValido(
                        jwtToken,
                        userDetails
                )) {

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authenticationToken);

                    log.debug(
                            "Usuario autenticado mediante JWT: {}",
                            username
                    );
                }
            }

        } catch (ExpiredJwtException ex) {

            log.warn(
                    "JWT expirado para URI: {}",
                    request.getRequestURI()
            );

        } catch (JwtException ex) {

            log.warn(
                    "JWT inválido para URI: {}",
                    request.getRequestURI()
            );

        } catch (Exception ex) {

            log.error(
                    "Error procesando autenticación JWT",
                    ex
            );
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}