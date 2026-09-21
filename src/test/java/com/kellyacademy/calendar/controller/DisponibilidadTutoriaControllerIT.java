package com.kellyacademy.calendar.controller;

import com.kellyacademy.calendar.dto.request.ActualizarDisponibilidadRequest;
import com.kellyacademy.calendar.dto.request.CrearDisponibilidadRequest;
import com.kellyacademy.calendar.dto.response.DisponibilidadResponse;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DisponibilidadTutoriaControllerIT extends IntegrationTestBase {

    private UUID estudianteId;
    private String estudianteToken;

    @BeforeEach
    void prepararEscenario() {
        estudianteId = crearUsuario("Estudiante", "Disp", "estudiante.disp.it@kellyacademy.com", "ESTUDIANTE");
        estudianteToken = login("estudiante.disp.it@kellyacademy.com", PASSWORD);
    }

    private CrearDisponibilidadRequest req(UUID docenteId, DayOfWeek dia, LocalTime hi, LocalTime hf) {
        return new CrearDisponibilidadRequest(docenteId, dia, hi, hf);
    }

    // -------- crear --------

    @Test
    void crear_comoDocenteDueno_devuelve201() {
        CrearDisponibilidadRequest req = req(docenteDuenoId, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        ResponseEntity<DisponibilidadResponse> resp =
                post("/api/disponibilidades", docenteDuenoToken, req, DisponibilidadResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).docenteId()).isEqualTo(docenteDuenoId);
        assertThat(resp.getBody().bloqueada()).isFalse();
    }

    @Test
    void crear_horaFinAntesDeInicio_devuelve400() {
        CrearDisponibilidadRequest req = req(docenteDuenoId, DayOfWeek.MONDAY,
                LocalTime.of(11, 0), LocalTime.of(10, 0));

        ResponseEntity<ErrorResponse> resp =
                post("/api/disponibilidades", docenteDuenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("HORAS_INVALIDAS");
    }

    @Test
    void crear_solape_devuelve400() {
        CrearDisponibilidadRequest primero = req(docenteDuenoId, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0));
        post("/api/disponibilidades", docenteDuenoToken, primero, DisponibilidadResponse.class);

        CrearDisponibilidadRequest solapado = req(docenteDuenoId, DayOfWeek.MONDAY,
                LocalTime.of(9, 30), LocalTime.of(10, 30));

        ResponseEntity<ErrorResponse> resp =
                post("/api/disponibilidades", docenteDuenoToken, solapado, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("BLOQUE_SOLAPADO");
    }

    @Test
    void crear_docenteAjeno_devuelve403() {
        // docenteAjenoToken intenta crear disponibilidad para docenteDuenoId.
        CrearDisponibilidadRequest req = req(docenteDuenoId, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        ResponseEntity<ErrorResponse> resp =
                post("/api/disponibilidades", docenteAjenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crear_comoEstudiante_devuelve403() {
        CrearDisponibilidadRequest req = req(estudianteId, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0));

        ResponseEntity<ErrorResponse> resp =
                post("/api/disponibilidades", estudianteToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crear_comoAdminParaOtroDocente_devuelve201() {
        CrearDisponibilidadRequest req = req(docenteAjenoId, DayOfWeek.TUESDAY,
                LocalTime.of(14, 0), LocalTime.of(15, 0));

        ResponseEntity<DisponibilidadResponse> resp =
                post("/api/disponibilidades", adminToken, req, DisponibilidadResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).docenteId()).isEqualTo(docenteAjenoId);
    }

    // -------- listar --------

    @Test
    void listar_comoEstudiante_puedeVerDisponibilidad() {
        CrearDisponibilidadRequest req = req(docenteDuenoId, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0));
        post("/api/disponibilidades", docenteDuenoToken, req, DisponibilidadResponse.class);

        ResponseEntity<String> resp =
                get("/api/disponibilidades?docenteId=" + docenteDuenoId, estudianteToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    @Test
    void listar_porDiaSemana_filtraCorrectamente() {
        post("/api/disponibilidades", docenteDuenoToken,
                req(docenteDuenoId, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                DisponibilidadResponse.class);
        post("/api/disponibilidades", docenteDuenoToken,
                req(docenteDuenoId, DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                DisponibilidadResponse.class);

        ResponseEntity<String> resp =
                get("/api/disponibilidades?diaSemana=MONDAY", docenteDuenoToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    // -------- obtener --------

    @Test
    void obtener_comoEstudiante_devuelve200() {
        DisponibilidadResponse creado = Objects.requireNonNull(
                post("/api/disponibilidades", docenteDuenoToken,
                        req(docenteDuenoId, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                        DisponibilidadResponse.class).getBody()
        );

        ResponseEntity<DisponibilidadResponse> resp =
                get("/api/disponibilidades/" + creado.id(), estudianteToken, DisponibilidadResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).id()).isEqualTo(creado.id());
    }

    // -------- actualizar --------

    @Test
    void actualizar_propio_devuelve200() {
        DisponibilidadResponse creado = Objects.requireNonNull(
                post("/api/disponibilidades", docenteDuenoToken,
                        req(docenteDuenoId, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                        DisponibilidadResponse.class).getBody()
        );

        ActualizarDisponibilidadRequest upd = new ActualizarDisponibilidadRequest(
                DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0), true
        );

        ResponseEntity<DisponibilidadResponse> resp =
                put("/api/disponibilidades/" + creado.id(), docenteDuenoToken, upd, DisponibilidadResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).diaSemana()).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(resp.getBody().bloqueada()).isTrue();
    }

    @Test
    void actualizar_ajeno_devuelve403() {
        DisponibilidadResponse creado = Objects.requireNonNull(
                post("/api/disponibilidades", docenteDuenoToken,
                        req(docenteDuenoId, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                        DisponibilidadResponse.class).getBody()
        );

        ActualizarDisponibilidadRequest upd = new ActualizarDisponibilidadRequest(
                DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0), false
        );

        ResponseEntity<ErrorResponse> resp =
                put("/api/disponibilidades/" + creado.id(), docenteAjenoToken, upd, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------- eliminar --------

    @Test
    void eliminar_comoAdmin_devuelve204() {
        DisponibilidadResponse creado = Objects.requireNonNull(
                post("/api/disponibilidades", docenteDuenoToken,
                        req(docenteDuenoId, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                        DisponibilidadResponse.class).getBody()
        );

        ResponseEntity<Void> resp = delete("/api/disponibilidades/" + creado.id(), adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void eliminar_comoDocente_devuelve403() {
        DisponibilidadResponse creado = Objects.requireNonNull(
                post("/api/disponibilidades", docenteDuenoToken,
                        req(docenteDuenoId, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                        DisponibilidadResponse.class).getBody()
        );

        ResponseEntity<ErrorResponse> resp =
                deleteWithBody("/api/disponibilidades/" + creado.id(), docenteDuenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}