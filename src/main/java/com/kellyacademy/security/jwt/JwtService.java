package com.kellyacademy.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CREDENTIAL_VERSION_CLAIM = "credential_version";
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
                // The password itself and its stored hash are never embedded in the JWT.
                .claim(CREDENTIAL_VERSION_CLAIM, credentialVersion(userDetails))
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

        String version = extraerClaim(token, claims -> claims.get(CREDENTIAL_VERSION_CLAIM, String.class));
        String currentVersion = credentialVersion(userDetails);
        return username.equals(userDetails.getUsername())
                && !estaExpirado(token)
                && version != null
                && MessageDigest.isEqual(version.getBytes(StandardCharsets.UTF_8),
                        currentVersion.getBytes(StandardCharsets.UTF_8));
    }

    // An HMAC of the persisted password hash changes on password reset without
    // disclosing the hash or requiring a schema change. Legacy tokens without
    // this claim are rejected, so deployment requires users to log in again.
    private String credentialVersion(UserDetails userDetails) {
        if (userDetails.getPassword() == null) {
            throw new IllegalArgumentException("No se puede generar una sesion sin credenciales");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(obtenerClave());
            byte[] input = ("kellyacademy:credential-version:" + userDetails.getPassword())
                    .getBytes(StandardCharsets.UTF_8);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(input));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo validar la version de credenciales", ex);
        }
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