package com.kellyacademy.security.terms;

import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TerminosControllerIT extends IntegrationTestBase {
    @Autowired private AceptacionTerminosRepository aceptaciones;

    @Test
    void usuarioPendienteDebeAceptarLaVersionYRolesMostradosAntesDeUsarLaApi() {
        UUID usuarioId = crearUsuario("Estudiante", "Prueba", "pendiente.it@kellyacademy.com", "ESTUDIANTE");
        AceptacionTerminos antigua = new AceptacionTerminos();
        antigua.setUsuarioId(usuarioId);
        antigua.setVersion("version-anterior");
        antigua.setRoles("ESTUDIANTE");
        antigua.setAceptadoEn(LocalDateTime.now().minusDays(1));
        aceptaciones.saveAndFlush(antigua);
        String token = loginSinAceptar("pendiente.it@kellyacademy.com", PASSWORD);
        TerminosService.Estado estado = Objects.requireNonNull(get("/api/terminos/aceptacion", token, TerminosService.Estado.class).getBody());
        assertThat(estado.aceptado()).isFalse();
        assertThat(estado.roles()).containsExactly("ESTUDIANTE");
        ResponseEntity<ErrorResponse> bloqueado = get("/api/recursos", token, ErrorResponse.class);
        assertThat(bloqueado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(Objects.requireNonNull(bloqueado.getBody()).getCodigo()).isEqualTo("TERMINOS_PENDIENTES");

        assertThat(post("/api/terminos/aceptacion", token, Map.of("version", "antigua", "rolesMostrados", estado.roles()), ErrorResponse.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(post("/api/terminos/aceptacion", token, Map.of("version", estado.version(), "rolesMostrados", List.of("DOCENTE")), ErrorResponse.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(aceptaciones.findFirstByUsuarioIdOrderByAceptadoEnDescIdDesc(usuarioId).orElseThrow().getVersion()).isEqualTo("version-anterior");
        TerminosService.Estado aceptado = Objects.requireNonNull(post("/api/terminos/aceptacion", token, Map.of("version", estado.version(), "rolesMostrados", estado.roles()), TerminosService.Estado.class).getBody());
        assertThat(aceptado.aceptado()).isTrue();
        assertThat(aceptado.aceptadoEn()).isNotNull();
        assertThat(get("/api/recursos", token, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        long antes = aceptaciones.count();
        assertThat(post("/api/terminos/aceptacion", token, Map.of("version", estado.version(), "rolesMostrados", estado.roles()), TerminosService.Estado.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aceptaciones.count()).isEqualTo(antes);
    }

    @Test
    void cambioDeRolesExigeAceptarElNuevoTextoYConservaElHistorial() {
        UUID id = crearUsuario("Estudiante", "Prueba", "cambio.it@kellyacademy.com", "ESTUDIANTE");
        String token = login("cambio.it@kellyacademy.com", PASSWORD);
        var usuario = usuarioRepository.findWithRolesById(id).orElseThrow();
        usuario.getRoles().add(rolRepository.findByNombre("DOCENTE").orElseThrow());
        usuarioRepository.saveAndFlush(usuario);

        assertThat(get("/api/recursos", token, ErrorResponse.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        TerminosService.Estado actual = Objects.requireNonNull(get("/api/terminos/aceptacion", token, TerminosService.Estado.class).getBody());
        assertThat(actual.aceptado()).isFalse();
        assertThat(actual.roles()).containsExactly("DOCENTE", "ESTUDIANTE");
        assertThat(post("/api/terminos/aceptacion", token, Map.of("version", actual.version(), "rolesMostrados", List.of("ESTUDIANTE")), ErrorResponse.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        long antes = aceptaciones.count();
        assertThat(post("/api/terminos/aceptacion", token, Map.of("version", actual.version(), "rolesMostrados", actual.roles()), TerminosService.Estado.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aceptaciones.count()).isEqualTo(antes + 1);
        assertThat(get("/api/recursos", token, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void rutasPublicasYPreflightNoSeBloqueanPorAceptacionPendiente() {
        crearUsuario("Estudiante", "Prueba", "publico.it@kellyacademy.com", "ESTUDIANTE");
        String token = loginSinAceptar("publico.it@kellyacademy.com", PASSWORD);
        assertThat(get("/actuator/health", token, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpHeaders preflight = headersConToken(token);
        preflight.setOrigin("http://localhost:4200");
        preflight.setAccessControlRequestMethod(HttpMethod.GET);
        ResponseEntity<String> resp = rest.exchange("/api/recursos", HttpMethod.OPTIONS, new HttpEntity<>(preflight), String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:4200");
        HttpHeaders solicitud = headersConToken(token);
        solicitud.setOrigin("http://localhost:4200");
        ResponseEntity<ErrorResponse> bloqueo = rest.exchange("/api/recursos", HttpMethod.GET, new HttpEntity<>(solicitud), ErrorResponse.class);
        assertThat(bloqueo.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(bloqueo.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:4200");
        ResponseEntity<String> anonimo = rest.getForEntity("/api/terminos/aceptacion", String.class);
        assertThat(anonimo.getStatusCode().value()).isIn(401, 403);
    }
}
