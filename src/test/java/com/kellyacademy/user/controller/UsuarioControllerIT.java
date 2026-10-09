package com.kellyacademy.user.controller;

import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import com.kellyacademy.user.dto.request.AsignarRolesUsuarioRequest;
import com.kellyacademy.user.dto.request.CambiarContrasenaRequest;
import com.kellyacademy.user.dto.request.CambiarEstadoUsuarioRequest;
import com.kellyacademy.user.dto.request.CrearUsuarioRequest;
import com.kellyacademy.user.dto.response.UsuarioResponse;
import com.kellyacademy.user.enums.EstadoUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;
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

    // ------------------------------------------------------------------
    // cambiarContrasena
    // ------------------------------------------------------------------

    @Test
    void cambiarContrasena_propioUsuario_devuelve204() {
        UUID usuarioId = crearEstudianteParaTests();
        String token = login("estudiante.estado.it@kellyacademy.com", PASSWORD);

        CambiarContrasenaRequest req = new CambiarContrasenaRequest(PASSWORD, "NuevaPass123");

        ResponseEntity<Void> resp = patch(
                "/api/usuarios/" + usuarioId + "/contrasena",
                token, req, Void.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void cambiarContrasena_actualIncorrecta_devuelve400() {
        UUID usuarioId = crearEstudianteParaTests();
        String token = login("estudiante.estado.it@kellyacademy.com", PASSWORD);

        CambiarContrasenaRequest req = new CambiarContrasenaRequest("Incorrecta123", "NuevaPass123");

        ResponseEntity<ErrorResponse> resp = patch(
                "/api/usuarios/" + usuarioId + "/contrasena",
                token, req, ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo())
                .isEqualTo("CONTRASENA_ACTUAL_INCORRECTA");
    }

    @Test
    void cambiarContrasena_nuevaIgualAActual_devuelve400() {
        UUID usuarioId = crearEstudianteParaTests();
        String token = login("estudiante.estado.it@kellyacademy.com", PASSWORD);

        CambiarContrasenaRequest req = new CambiarContrasenaRequest(PASSWORD, PASSWORD);

        ResponseEntity<ErrorResponse> resp = patch(
                "/api/usuarios/" + usuarioId + "/contrasena",
                token, req, ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo())
                .isEqualTo("CONTRASENA_SIN_CAMBIOS");
    }

    @Test
    void cambiarContrasena_otroUsuario_devuelve403() {
        UUID usuarioId = crearEstudianteParaTests();

        // docenteDueno intenta cambiar la contrasena del estudiante.
        CambiarContrasenaRequest req = new CambiarContrasenaRequest(PASSWORD, "NuevaPass123");

        ResponseEntity<ErrorResponse> resp = patch(
                "/api/usuarios/" + usuarioId + "/contrasena",
                docenteDuenoToken, req, ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cambiarContrasena_sinAutenticar_devuelve401() {
        UUID usuarioId = crearEstudianteParaTests();
        CambiarContrasenaRequest req = new CambiarContrasenaRequest(PASSWORD, "NuevaPass123");

        // Sin token: no usamos patch del base porque requiere token. Hacemos exchange crudo.
        ResponseEntity<ErrorResponse> resp = rest.exchange(
                "/api/usuarios/" + usuarioId + "/contrasena",
                HttpMethod.PATCH,
                new HttpEntity<>(req),
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    void cambiarContrasena_nuevaSinNumero_devuelve400() {
        UUID usuarioId = crearEstudianteParaTests();
        String token = login("estudiante.estado.it@kellyacademy.com", PASSWORD);

        CambiarContrasenaRequest req = new CambiarContrasenaRequest(PASSWORD, "SoloLetras");

        ResponseEntity<ErrorResponse> resp = patch(
                "/api/usuarios/" + usuarioId + "/contrasena",
                token, req, ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cambiarContrasena_puedeLoguearseConNueva() {
        UUID usuarioId = crearEstudianteParaTests();
        String token = login("estudiante.estado.it@kellyacademy.com", PASSWORD);

        CambiarContrasenaRequest req = new CambiarContrasenaRequest(PASSWORD, "NuevaPass123");

        patch("/api/usuarios/" + usuarioId + "/contrasena", token, req, Void.class);

        // Login con la nueva: debe funcionar.
        String nuevoToken = login("estudiante.estado.it@kellyacademy.com", "NuevaPass123");
        assertThat(nuevoToken).isNotBlank();

        // Login con la vieja: debe fallar.
        ResponseEntity<ErrorResponse> resp = rest.postForEntity(
                "/auth/login",
                new com.kellyacademy.security.auth.dto.AuthRequest() {{
                    setCorreoElectronico("estudiante.estado.it@kellyacademy.com");
                    setContrasena(PASSWORD);
                }},
                ErrorResponse.class
        );
        assertThat(resp.getStatusCode().value()).isEqualTo(401);
    }

    // ------------------------------------------------------------------
    // #70 - proteccion del ultimo admin activo
    // ------------------------------------------------------------------

    private String crearSegundoAdminYLogin(String correo) {
        CrearUsuarioRequest req = new CrearUsuarioRequest(
                "Admin", "Secundario", correo,
                PASSWORD, null, Set.of("ADMINISTRADOR")
        );
        ResponseEntity<UsuarioResponse> resp = post("/api/usuarios", adminToken, req, UsuarioResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return login(correo, PASSWORD);
    }

    @Test
    void cambiarEstado_conDosAdminsActivos_permiteDesactivarAUno() {
        String segundoAdminEmail = "admin2.estado.it@kellyacademy.com";
        // Creamos segundo admin (admin base sigue autenticado).
        CrearUsuarioRequest req = new CrearUsuarioRequest(
                "Admin", "Secundario", segundoAdminEmail,
                PASSWORD, null, Set.of("ADMINISTRADOR")
        );
        ResponseEntity<UsuarioResponse> creado = post("/api/usuarios", adminToken, req, UsuarioResponse.class);
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID segundoAdminId = Objects.requireNonNull(creado.getBody()).id();

        // Con 2 admins activos, desactivar al segundo debe permitirse.
        CambiarEstadoUsuarioRequest estadoReq = new CambiarEstadoUsuarioRequest(EstadoUsuario.BLOQUEADO);
        ResponseEntity<UsuarioResponse> resp = rest.exchange(
                "/api/usuarios/" + segundoAdminId + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>(estadoReq, headersConToken(adminToken)),
                UsuarioResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).estado()).isEqualTo(EstadoUsuario.BLOQUEADO);
    }

    @Test
    void asignarRoles_conDosAdminsActivos_permiteQuitarAdminAUno() {
        String segundoAdminEmail = "admin3.estado.it@kellyacademy.com";
        CrearUsuarioRequest req = new CrearUsuarioRequest(
                "Admin", "Terciario", segundoAdminEmail,
                PASSWORD, null, Set.of("ADMINISTRADOR")
        );
        ResponseEntity<UsuarioResponse> creado = post("/api/usuarios", adminToken, req, UsuarioResponse.class);
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID segundoAdminId = Objects.requireNonNull(creado.getBody()).id();

        // Con 2 admins activos, quitarle ADMINISTRADOR al segundo debe permitirse.
        AsignarRolesUsuarioRequest rolesReq = new AsignarRolesUsuarioRequest(Set.of("ESTUDIANTE"));
        ResponseEntity<UsuarioResponse> resp = rest.exchange(
                "/api/usuarios/" + segundoAdminId + "/roles",
                HttpMethod.PUT,
                new HttpEntity<>(rolesReq, headersConToken(adminToken)),
                UsuarioResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).roles()).containsExactly("ESTUDIANTE");
    }

    @Test
    void eliminar_conDosAdminsActivos_permiteEliminarAUno() {
        String segundoAdminEmail = "admin4.estado.it@kellyacademy.com";
        CrearUsuarioRequest req = new CrearUsuarioRequest(
                "Admin", "Cuaternario", segundoAdminEmail,
                PASSWORD, null, Set.of("ADMINISTRADOR")
        );
        ResponseEntity<UsuarioResponse> creado = post("/api/usuarios", adminToken, req, UsuarioResponse.class);
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID segundoAdminId = Objects.requireNonNull(creado.getBody()).id();

        // Con 2 admins activos, eliminar al segundo debe permitirse.
        ResponseEntity<Void> resp = delete("/api/usuarios/" + segundoAdminId, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}