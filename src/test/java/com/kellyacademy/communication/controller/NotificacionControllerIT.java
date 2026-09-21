package com.kellyacademy.communication.controller;

import com.kellyacademy.communication.dto.response.NotificacionResponse;
import com.kellyacademy.communication.entity.Notificacion;
import com.kellyacademy.communication.enums.TipoNotificacion;
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

class NotificacionControllerIT extends IntegrationTestBase {

    @Autowired private NotificacionRepository notificacionRepository;

    private UUID usuarioId;
    private UUID usuarioAjenoId;
    private String usuarioToken;
    private String usuarioAjenoToken;

    @BeforeEach
    void prepararEscenario() {
        usuarioId = crearUsuario("Ana", "Torres", "ana.notif.it@kellyacademy.com", "ESTUDIANTE");
        usuarioAjenoId = crearUsuario("Luis", "Perez", "luis.notif.it@kellyacademy.com", "ESTUDIANTE");
        usuarioToken = login("ana.notif.it@kellyacademy.com", PASSWORD);
        usuarioAjenoToken = login("luis.notif.it@kellyacademy.com", PASSWORD);
    }

    // Crea una notificacion directamente por repositorio (no hay endpoint de creacion).
    private UUID crearNotificacion(UUID usuarioId, TipoNotificacion tipo, boolean leida) {
        Notificacion n = new Notificacion();
        n.setUsuario(usuarioRepository.findById(usuarioId).orElseThrow());
        n.setTipo(tipo);
        n.setTitulo("Titulo " + tipo);
        n.setCuerpo("Cuerpo");
        n.setLink(null);
        n.setLeida(leida);
        return notificacionRepository.save(n).getId();
    }

    // -------- listar --------

    @Test
    void listar_comoUsuario_devuelveSoloLasSuyas() {
        crearNotificacion(usuarioId, TipoNotificacion.SISTEMA, false);
        crearNotificacion(usuarioAjenoId, TipoNotificacion.SISTEMA, false);

        ResponseEntity<String> resp = get("/api/notificaciones", usuarioToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    @Test
    void listar_comoAdmin_devuelveTodas() {
        crearNotificacion(usuarioId, TipoNotificacion.SISTEMA, false);
        crearNotificacion(usuarioAjenoId, TipoNotificacion.SISTEMA, false);

        ResponseEntity<String> resp = get("/api/notificaciones", adminToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":2");
    }

    @Test
    void listar_sinToken_devuelve401() {
        ResponseEntity<ErrorResponse> resp = rest.getForEntity(
                "/api/notificaciones", ErrorResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // -------- no-leidas --------

    @Test
    void noLeidas_devuelveSoloNoLeidas() {
        crearNotificacion(usuarioId, TipoNotificacion.SISTEMA, false);
        crearNotificacion(usuarioId, TipoNotificacion.ANUNCIO, true);

        ResponseEntity<String> resp = get("/api/notificaciones/no-leidas", usuarioToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    // -------- count-no-leidas --------

    @Test
    void countNoLeidas_devuelveConteo() {
        crearNotificacion(usuarioId, TipoNotificacion.SISTEMA, false);
        crearNotificacion(usuarioId, TipoNotificacion.MENSAJE, false);

        ResponseEntity<String> resp = get("/api/notificaciones/count-no-leidas", usuarioToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"total\":2");
    }

    // -------- obtener --------

    @Test
    void obtener_cuandoEsDueno_devuelve200() {
        UUID id = crearNotificacion(usuarioId, TipoNotificacion.SISTEMA, false);

        ResponseEntity<NotificacionResponse> resp =
                get("/api/notificaciones/" + id, usuarioToken, NotificacionResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).id()).isEqualTo(id);
    }

    @Test
    void obtener_cuandoEsAjeno_devuelve403() {
        UUID id = crearNotificacion(usuarioId, TipoNotificacion.SISTEMA, false);

        ResponseEntity<ErrorResponse> resp =
                get("/api/notificaciones/" + id, usuarioAjenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------- marcar leida --------

    @Test
    void marcarLeida_cuandoEsDueno_devuelve200() {
        UUID id = crearNotificacion(usuarioId, TipoNotificacion.SISTEMA, false);

        ResponseEntity<NotificacionResponse> resp =
                patch("/api/notificaciones/" + id + "/leer", usuarioToken, NotificacionResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).leida()).isTrue();
    }

    @Test
    void marcarLeida_cuandoEsAjeno_devuelve403() {
        UUID id = crearNotificacion(usuarioId, TipoNotificacion.SISTEMA, false);

        ResponseEntity<ErrorResponse> resp =
                patch("/api/notificaciones/" + id + "/leer", usuarioAjenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------- leer todas --------

    @Test
    void leerTodas_marcaTodasLasNoLeidas() {
        crearNotificacion(usuarioId, TipoNotificacion.SISTEMA, false);
        crearNotificacion(usuarioId, TipoNotificacion.MENSAJE, false);

        ResponseEntity<Void> resp = patch("/api/notificaciones/leer-todas", usuarioToken, Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId)).isZero();
    }

    // -------- eliminar --------

    @Test
    void eliminar_cuandoEsDueno_devuelve204() {
        UUID id = crearNotificacion(usuarioId, TipoNotificacion.SISTEMA, false);

        ResponseEntity<Void> resp = delete("/api/notificaciones/" + id, usuarioToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(notificacionRepository.findById(id)).isEmpty();
    }

    @Test
    void eliminar_cuandoEsAjeno_devuelve403() {
        UUID id = crearNotificacion(usuarioId, TipoNotificacion.SISTEMA, false);

        ResponseEntity<ErrorResponse> resp =
                deleteWithBody("/api/notificaciones/" + id, usuarioAjenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void eliminar_cuandoEsAdmin_permite() {
        UUID id = crearNotificacion(usuarioId, TipoNotificacion.SISTEMA, false);

        ResponseEntity<Void> resp = delete("/api/notificaciones/" + id, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}