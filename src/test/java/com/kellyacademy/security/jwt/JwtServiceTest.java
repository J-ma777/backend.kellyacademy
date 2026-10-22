package com.kellyacademy.security.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    private JwtService service(long expiration, String secret) {
        var p = new JwtProperties(); p.setExpiration(expiration);
        p.setSecret(Base64.getEncoder().encodeToString(secret.getBytes()));
        return new JwtService(p);
    }
    private JwtService service(long expiration) { return service(expiration, "clave-de-pruebas-aisladas-jwt-12345678901234567890"); }
    private final org.springframework.security.core.userdetails.UserDetails user = User.withUsername("ana@it.invalid").password("irrelevante").roles("ESTUDIANTE").build();

    @Test void emiteTokenFirmadoConUsuarioClaimsYExpiracion() {
        var service = service(3600000L);
        String token = service.generarToken(Map.of("curso", "B1", "sub", "no-debe-prevalecer"), user);
        assertThat(service.extraerNombreUsuario(token)).isEqualTo(user.getUsername());
        String curso = service.extraerClaim(token, c -> c.get("curso", String.class));
        assertThat(curso).isEqualTo("B1");
        assertThat(service.extraerClaim(token, Claims::getExpiration)).isAfter(new Date());
        assertThat(service.esTokenValido(token, user)).isTrue();
    }
    @Test void noAceptaTokenDeOtroUsuario() {
        var service = service(3600000L);
        var other = User.withUsername("otro@it.invalid").password("irrelevante").roles("ESTUDIANTE").build();
        assertThat(service.esTokenValido(service.generarToken(user), other)).isFalse();
    }
    @Test void rechazaTokensExpiradosSinEsperasDeReloj() {
        var service = service(-3600000L);
        assertThatThrownBy(() -> service.esTokenValido(service.generarToken(user), user)).isInstanceOf(ExpiredJwtException.class);
    }
    @Test void rechazaFirmaAjenaYFormatoManipulado() {
        var service = service(3600000L);
        String ajeno = service(3600000L, "otra-clave-de-pruebas-totalmente-distinta-1234567890").generarToken(user);
        assertThatThrownBy(() -> service.extraerNombreUsuario(ajeno)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.extraerNombreUsuario("no.es.jwt")).isInstanceOf(JwtException.class);
    }
}
