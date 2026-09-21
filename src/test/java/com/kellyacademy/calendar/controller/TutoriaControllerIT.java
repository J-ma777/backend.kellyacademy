package com.kellyacademy.calendar.controller;

import com.kellyacademy.calendar.dto.request.ActualizarTutoriaRequest;
import com.kellyacademy.calendar.dto.request.CrearTutoriaRequest;
import com.kellyacademy.calendar.dto.response.TutoriaResponse;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TutoriaControllerIT extends IntegrationTestBase {

    private UUID estudianteId;
    private String estudianteToken;
    private String terceroToken;

    @BeforeEach
    void prepararEscenario() {
        estudianteId = crearUsuario("Estudiante", "Tut", "est.tutoria.it@kellyacademy.com", "ESTUDIANTE");
        estudianteToken = login("est.tutoria.it@kellyacademy.com", PASSWORD);
        crearUsuario("Otro", "User", "otro.tutoria.it@kellyacademy.com", "ESTUDIANTE");
        terceroToken = login("otro.tutoria.it@kellyacademy.com", PASSWORD);
    }

    private CrearTutoriaRequest req(UUID estudianteId, UUID docenteId, LocalDateTime fecha) {
        return new CrearTutoriaRequest(estudianteId, docenteId, null, fecha, 60);
    }

    // -------- crear --------

    @Test
    void crear_comoEstudiante_devuelve201() {
        CrearTutoriaRequest req = req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1));

        ResponseEntity<TutoriaResponse> resp =
                post("/api/tutorias", estudianteToken, req, TutoriaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).estado().name()).isEqualTo("PENDIENTE");
        assertThat(resp.getBody().estudianteId()).isEqualTo(estudianteId);
        assertThat(resp.getBody().docenteId()).isEqualTo(docenteDuenoId);
    }

    @Test
    void crear_comoDocente_devuelve201() {
        CrearTutoriaRequest req = req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1));

        ResponseEntity<TutoriaResponse> resp =
                post("/api/tutorias", docenteDuenoToken, req, TutoriaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void crear_comoTercero_devuelve403() {
        CrearTutoriaRequest req = req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1));

        ResponseEntity<ErrorResponse> resp =
                post("/api/tutorias", terceroToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crear_fechaPasada_devuelve400() {
        CrearTutoriaRequest req = req(estudianteId, docenteDuenoId, LocalDateTime.now().minusDays(1));

        ResponseEntity<ErrorResponse> resp =
                post("/api/tutorias", estudianteToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("TUTORIA_FECHA_PASADA");
    }

    @Test
    void crear_comoAdmin_ok() {
        CrearTutoriaRequest req = req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1));

        ResponseEntity<TutoriaResponse> resp =
                post("/api/tutorias", adminToken, req, TutoriaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // -------- listar --------

    @Test
    void listar_comoEstudiante_soloVeLasSuyas() {
        // Tutoria 1: estudiante + docenteDueno. El estudiante participa.
        post("/api/tutorias", estudianteToken,
                req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1)),
                TutoriaResponse.class);
        // Tutoria 2: otro estudiante + docenteAjeno. El estudiante autenticado NO participa.
        UUID otroEstudianteId = crearUsuario("Otro", "Est", "otro.est.tut.it@kellyacademy.com", "ESTUDIANTE");
        post("/api/tutorias", docenteAjenoToken,
                req(otroEstudianteId, docenteAjenoId, LocalDateTime.now().plusDays(2)),
                TutoriaResponse.class);

        ResponseEntity<String> resp = get("/api/tutorias", estudianteToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    @Test
    void listar_comoAdmin_veTodas() {
        post("/api/tutorias", estudianteToken,
                req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1)),
                TutoriaResponse.class);
        UUID otroEstudianteId = crearUsuario("Otro", "Est", "otro.est.tut2.it@kellyacademy.com", "ESTUDIANTE");
        post("/api/tutorias", docenteAjenoToken,
                req(otroEstudianteId, docenteAjenoId, LocalDateTime.now().plusDays(2)),
                TutoriaResponse.class);

        ResponseEntity<String> resp = get("/api/tutorias", adminToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":2");
    }

    @Test
    void listar_filtroPorEstado() {
        post("/api/tutorias", estudianteToken,
                req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1)),
                TutoriaResponse.class);

        ResponseEntity<String> resp =
                get("/api/tutorias?estado=PENDIENTE", estudianteToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    // -------- obtener --------

    @Test
    void obtener_comoEstudiante_200() {
        TutoriaResponse creada = Objects.requireNonNull(
                post("/api/tutorias", estudianteToken,
                        req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1)),
                        TutoriaResponse.class).getBody()
        );

        ResponseEntity<TutoriaResponse> resp =
                get("/api/tutorias/" + creada.id(), estudianteToken, TutoriaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void obtener_comoTercero_403() {
        TutoriaResponse creada = Objects.requireNonNull(
                post("/api/tutorias", estudianteToken,
                        req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1)),
                        TutoriaResponse.class).getBody()
        );

        ResponseEntity<ErrorResponse> resp =
                get("/api/tutorias/" + creada.id(), terceroToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------- actualizar --------

    @Test
    void actualizar_pendiente_cambiaFecha_200() {
        TutoriaResponse creada = Objects.requireNonNull(
                post("/api/tutorias", estudianteToken,
                        req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1)),
                        TutoriaResponse.class).getBody()
        );

        ActualizarTutoriaRequest upd = new ActualizarTutoriaRequest(
                LocalDateTime.now().plusDays(5), 90, "nota editada"
        );

        ResponseEntity<TutoriaResponse> resp =
                put("/api/tutorias/" + creada.id(), estudianteToken, upd, TutoriaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).duracionMinutos()).isEqualTo(90);
    }

    @Test
    void actualizar_comoTercero_403() {
        TutoriaResponse creada = Objects.requireNonNull(
                post("/api/tutorias", estudianteToken,
                        req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1)),
                        TutoriaResponse.class).getBody()
        );

        ActualizarTutoriaRequest upd = new ActualizarTutoriaRequest(
                LocalDateTime.now().plusDays(5), 90, null
        );

        ResponseEntity<ErrorResponse> resp =
                put("/api/tutorias/" + creada.id(), terceroToken, upd, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------- eliminar --------

    @Test
    void eliminar_comoAdmin_204() {
        TutoriaResponse creada = Objects.requireNonNull(
                post("/api/tutorias", estudianteToken,
                        req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1)),
                        TutoriaResponse.class).getBody()
        );

        ResponseEntity<Void> resp = delete("/api/tutorias/" + creada.id(), adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void eliminar_comoEstudiante_403() {
        TutoriaResponse creada = Objects.requireNonNull(
                post("/api/tutorias", estudianteToken,
                        req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1)),
                        TutoriaResponse.class).getBody()
        );

        ResponseEntity<ErrorResponse> resp =
                deleteWithBody("/api/tutorias/" + creada.id(), estudianteToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}