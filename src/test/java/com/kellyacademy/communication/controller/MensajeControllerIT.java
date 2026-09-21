package com.kellyacademy.communication.controller;

import com.kellyacademy.communication.dto.request.CrearConversacionRequest;
import com.kellyacademy.communication.dto.request.CrearMensajeRequest;
import com.kellyacademy.communication.dto.response.ConversacionResponse;
import com.kellyacademy.communication.dto.response.MensajeResponse;
import com.kellyacademy.communication.repository.NotificacionRepository;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MensajeControllerIT extends IntegrationTestBase {

    @Autowired private NotificacionRepository notificacionRepository;

    private UUID otroId;
    private UUID conversacionId;
    private String otroToken;

    @BeforeEach
    void prepararEscenario() {
        otroId = crearUsuario("Otro", "Msg", "otro.msg.it@kellyacademy.com", "ESTUDIANTE");
        otroToken = login("otro.msg.it@kellyacademy.com", PASSWORD);

        CrearConversacionRequest req = new CrearConversacionRequest(null, otroId, "Chat");
        ConversacionResponse conv = post(
                "/api/conversaciones", docenteDuenoToken, req, ConversacionResponse.class
        ).getBody();
        conversacionId = Objects.requireNonNull(conv).id();
    }

    @Test
    void crear_comoParticipante_devuelve201YNotifica() {
        CrearMensajeRequest req = new CrearMensajeRequest("Hola", null);

        ResponseEntity<MensajeResponse> resp = post(
                "/api/conversaciones/" + conversacionId + "/mensajes",
                docenteDuenoToken, req, MensajeResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).cuerpo()).isEqualTo("Hola");

        // El otro participante debe tener 1 notificacion.
        assertThat(notificacionRepository.countByUsuarioIdAndLeidaFalse(otroId)).isEqualTo(1L);
    }

    @Test
    void crear_comoUsuarioAjeno_devuelve403() {
        CrearMensajeRequest req = new CrearMensajeRequest("Hola", null);

        ResponseEntity<ErrorResponse> resp = post(
                "/api/conversaciones/" + conversacionId + "/mensajes",
                docenteAjenoToken, req, ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listar_comoParticipante_devuelveMensajes() {
        CrearMensajeRequest req = new CrearMensajeRequest("Hola", null);
        post("/api/conversaciones/" + conversacionId + "/mensajes",
                docenteDuenoToken, req, MensajeResponse.class);

        ResponseEntity<String> resp = get(
                "/api/conversaciones/" + conversacionId + "/mensajes",
                docenteDuenoToken, String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    @Test
    void listar_comoUsuarioAjeno_devuelve403() {
        ResponseEntity<ErrorResponse> resp = get(
                "/api/conversaciones/" + conversacionId + "/mensajes",
                docenteAjenoToken, ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void marcarLeido_comoDestinatario_devuelve200() {
        CrearMensajeRequest req = new CrearMensajeRequest("Hola", null);
        MensajeResponse creado = post(
                "/api/conversaciones/" + conversacionId + "/mensajes",
                docenteDuenoToken, req, MensajeResponse.class
        ).getBody();
        UUID mensajeId = Objects.requireNonNull(creado).id();

        ResponseEntity<MensajeResponse> resp = patch(
                "/api/conversaciones/" + conversacionId + "/mensajes/" + mensajeId + "/leer",
                otroToken, MensajeResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).leido()).isTrue();
    }

    @Test
    void leerTodos_marcaTodos() {
        CrearMensajeRequest req = new CrearMensajeRequest("Hola", null);
        post("/api/conversaciones/" + conversacionId + "/mensajes",
                docenteDuenoToken, req, MensajeResponse.class);

        ResponseEntity<Void> resp = patch(
                "/api/conversaciones/" + conversacionId + "/leer-todos",
                otroToken, Void.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}