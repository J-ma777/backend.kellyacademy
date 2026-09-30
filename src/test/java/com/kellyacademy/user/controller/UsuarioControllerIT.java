package com.kellyacademy.user.controller;

import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import com.kellyacademy.user.dto.request.AsignarRolesUsuarioRequest;
import com.kellyacademy.user.dto.request.CambiarEstadoUsuarioRequest;
import com.kellyacademy.user.dto.request.CrearUsuarioRequest;
import com.kellyacademy.user.dto.response.UsuarioResponse;
import com.kellyacademy.user.enums.EstadoUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioControllerIT extends IntegrationTestBase {

    private UUID crearEstudianteParaTests() {
        CrearUsuarioRequest req = new CrearUsuarioRequest(
                "Estudiante", "IT", "estudiante.estado.it@kellyacademy.com",
                PASSWORD, null, Set.of("ESTUDIANTE")
        );
        ResponseEntity<UsuarioResponse> resp = post("/api/usuarios", adminToken, req, UsuarioResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    @Test
    void cambiarEstado_comoAdmin_activoABloqueado_devuelve200() {
        UUID usuarioId = crearEstudianteParaTests();
        CambiarEstadoUsuarioRequest req = new CambiarEstadoUsuarioRequest(EstadoUsuario.BLOQUEADO);

        ResponseEntity<UsuarioResponse> resp = rest.exchange(
                "/api/usuarios/" + usuarioId + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>(req, headersConToken(adminToken)),
                UsuarioResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().estado()).isEqualTo(EstadoUsuario.BLOQUEADO);
    }

    @Test
    void cambiarEstado_comoDocente_devuelve403() {
        UUID usuarioId = crearEstudianteParaTests();
        CambiarEstadoUsuarioRequest req = new CambiarEstadoUsuarioRequest(EstadoUsuario.BLOQUEADO);

        ResponseEntity<ErrorResponse> resp = rest.exchange(
                "/api/usuarios/" + usuarioId + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>(req, headersConToken(docenteDuenoToken)),
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cambiarEstado_aSiMismo_devuelve400() {
        CambiarEstadoUsuarioRequest req = new CambiarEstadoUsuarioRequest(EstadoUsuario.INACTIVO);

        ResponseEntity<ErrorResponse> resp = rest.exchange(
                "/api/usuarios/" + adminId + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>(req, headersConToken(adminToken)),
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("NO_PUEDE_AUTOCAMBIAR_ESTADO");
    }

    @Test
    void cambiarEstado_mismoEstado_devuelve400() {
        UUID usuarioId = crearEstudianteParaTests();
        CambiarEstadoUsuarioRequest req = new CambiarEstadoUsuarioRequest(EstadoUsuario.ACTIVO);

        ResponseEntity<ErrorResponse> resp = rest.exchange(
                "/api/usuarios/" + usuarioId + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>(req, headersConToken(adminToken)),
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("ESTADO_SIN_CAMBIOS");
    }

    @Test
    void cambiarEstado_usuarioInexistente_devuelve404() {
        CambiarEstadoUsuarioRequest req = new CambiarEstadoUsuarioRequest(EstadoUsuario.BLOQUEADO);

        ResponseEntity<ErrorResponse> resp = rest.exchange(
                "/api/usuarios/" + UUID.randomUUID() + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>(req, headersConToken(adminToken)),
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cambiarEstado_sinEstado_devuelve400() {
        UUID usuarioId = crearEstudianteParaTests();

        ResponseEntity<ErrorResponse> resp = rest.exchange(
                "/api/usuarios/" + usuarioId + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>("{}", headersConToken(adminToken)),
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void asignarRoles_comoAdmin_reemplazaRoles_devuelve200() {
        UUID usuarioId = crearEstudianteParaTests();
        AsignarRolesUsuarioRequest req = new AsignarRolesUsuarioRequest(Set.of("DOCENTE"));

        ResponseEntity<UsuarioResponse> resp = rest.exchange(
                "/api/usuarios/" + usuarioId + "/roles",
                HttpMethod.PUT,
                new HttpEntity<>(req, headersConToken(adminToken)),
                UsuarioResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().roles()).containsExactly("DOCENTE");
    }

    @Test
    void asignarRoles_comoDocente_devuelve403() {
        UUID usuarioId = crearEstudianteParaTests();
        AsignarRolesUsuarioRequest req = new AsignarRolesUsuarioRequest(Set.of("DOCENTE"));

        ResponseEntity<ErrorResponse> resp = rest.exchange(
                "/api/usuarios/" + usuarioId + "/roles",
                HttpMethod.PUT,
                new HttpEntity<>(req, headersConToken(docenteDuenoToken)),
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void asignarRoles_aSiMismo_devuelve400() {
        AsignarRolesUsuarioRequest req = new AsignarRolesUsuarioRequest(Set.of("ESTUDIANTE"));

        ResponseEntity<ErrorResponse> resp = rest.exchange(
                "/api/usuarios/" + adminId + "/roles",
                HttpMethod.PUT,
                new HttpEntity<>(req, headersConToken(adminToken)),
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("NO_PUEDE_AUTOCAMBIAR_ROLES");
    }

    @Test
    void asignarRoles_rolInexistente_devuelve400() {
        UUID usuarioId = crearEstudianteParaTests();
        AsignarRolesUsuarioRequest req = new AsignarRolesUsuarioRequest(Set.of("ROL_FANTASMA"));

        ResponseEntity<ErrorResponse> resp = rest.exchange(
                "/api/usuarios/" + usuarioId + "/roles",
                HttpMethod.PUT,
                new HttpEntity<>(req, headersConToken(adminToken)),
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("ROL_INEXISTENTE");
    }

    @Test
    void asignarRoles_setVacio_devuelve400() {
        UUID usuarioId = crearEstudianteParaTests();
        AsignarRolesUsuarioRequest req = new AsignarRolesUsuarioRequest(Set.of());

        ResponseEntity<ErrorResponse> resp = rest.exchange(
                "/api/usuarios/" + usuarioId + "/roles",
                HttpMethod.PUT,
                new HttpEntity<>(req, headersConToken(adminToken)),
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}