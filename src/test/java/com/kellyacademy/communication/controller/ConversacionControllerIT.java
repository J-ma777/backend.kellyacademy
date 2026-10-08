package com.kellyacademy.communication.controller;

import com.kellyacademy.communication.dto.request.CambiarAsuntoConversacionRequest;
import com.kellyacademy.communication.dto.request.CrearConversacionRequest;
import com.kellyacademy.communication.dto.response.ConversacionResponse;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConversacionControllerIT extends IntegrationTestBase {

    private UUID otroId;
    private String otroToken;

    @BeforeEach
    void prepararEscenario() {
        otroId = crearUsuario("Otro", "User", "otro.conv.it@kellyacademy.com", "ESTUDIANTE");
        otroToken = login("otro.conv.it@kellyacademy.com", PASSWORD);
    }

    @Test
    void crear_comoDocente_devuelve201() {
        CrearConversacionRequest req = new CrearConversacionRequest(
                null, otroId, "Consulta"
        );

        ResponseEntity<ConversacionResponse> resp =
                post("/api/conversaciones", docenteDuenoToken, req, ConversacionResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).asunto()).isEqualTo("Consulta");
    }

    @Test
    void crear_autoconversacion_devuelve400() {
        CrearConversacionRequest req = new CrearConversacionRequest(
                null, docenteDuenoId, "Consulta"
        );

        ResponseEntity<ErrorResponse> resp =
                post("/api/conversaciones", docenteDuenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("NO_AUTOCONVERSACION");
    }

    @Test
    void crear_cuandoYaExiste_devuelve200O201Idempotente() {
        CrearConversacionRequest req = new CrearConversacionRequest(null, otroId, "Consulta");

        ResponseEntity<ConversacionResponse> primera =
                post("/api/conversaciones", docenteDuenoToken, req, ConversacionResponse.class);
        ResponseEntity<ConversacionResponse> segunda =
                post("/api/conversaciones", docenteDuenoToken, req, ConversacionResponse.class);

        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(segunda.getBody()).id())
                .isEqualTo(Objects.requireNonNull(primera.getBody()).id());
    }

    @Test
    void listar_devuelveSoloPropias() {
        CrearConversacionRequest req = new CrearConversacionRequest(null, otroId, "Consulta");
        post("/api/conversaciones", docenteDuenoToken, req, ConversacionResponse.class);

        ResponseEntity<String> resp = get("/api/conversaciones", docenteDuenoToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    @Test
    void listar_comoUsuarioAjeno_noVeConversacionesDeOtros() {
        CrearConversacionRequest req = new CrearConversacionRequest(null, otroId, "Consulta");
        post("/api/conversaciones", docenteDuenoToken, req, ConversacionResponse.class);

        ResponseEntity<String> resp = get("/api/conversaciones", docenteAjenoToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":0");
    }

    @Test
    void obtener_cuandoEsParticipante_devuelve200() {
        CrearConversacionRequest req = new CrearConversacionRequest(null, otroId, "Consulta");
        ConversacionResponse creada = post(
                "/api/conversaciones", docenteDuenoToken, req, ConversacionResponse.class
        ).getBody();
        UUID id = Objects.requireNonNull(creada).id();

        ResponseEntity<ConversacionResponse> resp =
                get("/api/conversaciones/" + id, docenteDuenoToken, ConversacionResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void obtener_cuandoNoEsParticipante_devuelve403() {
        CrearConversacionRequest req = new CrearConversacionRequest(null, otroId, "Consulta");
        ConversacionResponse creada = post(
                "/api/conversaciones", docenteDuenoToken, req, ConversacionResponse.class
        ).getBody();
        UUID id = Objects.requireNonNull(creada).id();

        ResponseEntity<ErrorResponse> resp =
                get("/api/conversaciones/" + id, docenteAjenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------- cambiarAsunto --------

    @Test
    void cambiarAsunto_comoParticipante_200() {
        CrearConversacionRequest req = new CrearConversacionRequest(null, otroId, "Original");
        ConversacionResponse creada = post(
                "/api/conversaciones", docenteDuenoToken, req, ConversacionResponse.class
        ).getBody();
        UUID id = Objects.requireNonNull(creada).id();

        CambiarAsuntoConversacionRequest cambio =
                new CambiarAsuntoConversacionRequest("Renombrada");

        ResponseEntity<ConversacionResponse> resp = patch(
                "/api/conversaciones/" + id + "/asunto",
                docenteDuenoToken, cambio, ConversacionResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).asunto()).isEqualTo("Renombrada");
    }

    @Test
    void cambiarAsunto_comoNoParticipante_403() {
        CrearConversacionRequest req = new CrearConversacionRequest(null, otroId, "Original");
        ConversacionResponse creada = post(
                "/api/conversaciones", docenteDuenoToken, req, ConversacionResponse.class
        ).getBody();
        UUID id = Objects.requireNonNull(creada).id();

        CambiarAsuntoConversacionRequest cambio =
                new CambiarAsuntoConversacionRequest("Renombrada");

        ResponseEntity<ErrorResponse> resp = patch(
                "/api/conversaciones/" + id + "/asunto",
                docenteAjenoToken, cambio, ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cambiarAsunto_mismoAsunto_400() {
        CrearConversacionRequest req = new CrearConversacionRequest(null, otroId, "Original");
        ConversacionResponse creada = post(
                "/api/conversaciones", docenteDuenoToken, req, ConversacionResponse.class
        ).getBody();
        UUID id = Objects.requireNonNull(creada).id();

        CambiarAsuntoConversacionRequest cambio =
                new CambiarAsuntoConversacionRequest("Original");

        ResponseEntity<ErrorResponse> resp = patch(
                "/api/conversaciones/" + id + "/asunto",
                docenteDuenoToken, cambio, ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo())
                .isEqualTo("ASUNTO_SIN_CAMBIOS");
    }

    @Test
    void cambiarAsunto_asuntoVacio_400() {
        CrearConversacionRequest req = new CrearConversacionRequest(null, otroId, "Original");
        ConversacionResponse creada = post(
                "/api/conversaciones", docenteDuenoToken, req, ConversacionResponse.class
        ).getBody();
        UUID id = Objects.requireNonNull(creada).id();

        CambiarAsuntoConversacionRequest cambio =
                new CambiarAsuntoConversacionRequest("");

        ResponseEntity<ErrorResponse> resp = patch(
                "/api/conversaciones/" + id + "/asunto",
                docenteDuenoToken, cambio, ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cambiarAsunto_conversacionInexistente_404() {
        CambiarAsuntoConversacionRequest cambio =
                new CambiarAsuntoConversacionRequest("X");

        ResponseEntity<ErrorResponse> resp = patch(
                "/api/conversaciones/" + UUID.randomUUID() + "/asunto",
                docenteDuenoToken, cambio, ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}