package com.kellyacademy.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String generarToken(UserDetails userDetails) {

        return generarToken(
                new HashMap<>(),
                userDetails
        );
    }

    public String generarToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {

        Date fechaActual = new Date();

        Date fechaExpiracion = new Date(
                fechaActual.getTime() +
                        jwtProperties.getExpiration()
        );

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(fechaActual)
                .expiration(fechaExpiracion)
                .signWith(obtenerClave())
                .compact();
    }

    public String extraerNombreUsuario(
            String token
    ) {

        return extraerClaim(
                token,
                Claims::getSubject
        );
    }

    public <T> T extraerClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {

        Claims claims = extraerTodosLosClaims(token);

        return claimsResolver.apply(claims);
    }

    public boolean esTokenValido(
            String token,
            UserDetails userDetails
    ) {

        String username =
                extraerNombreUsuario(token);

        return username.equals(
                userDetails.getUsername()
        ) && !estaExpirado(token);
    }

    private boolean estaExpirado(
            String token
    ) {

        return extraerExpiracion(token)
                .before(new Date());
    }

    private Date extraerExpiracion(
            String token
    ) {

        return extraerClaim(
                token,
                Claims::getExpiration
        );
    }

    private Claims extraerTodosLosClaims(
            String token
    ) {

        return Jwts.parser()
                .verifyWith(obtenerClave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey obtenerClave() {

        byte[] keyBytes =
                Decoders.BASE64.decode(
                        jwtProperties.getSecret()
                );

        return Keys.hmacShaKeyFor(keyBytes);
    }
}