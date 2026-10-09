package com.kellyacademy.calendar.controller;

import com.kellyacademy.calendar.dto.request.ActualizarTutoriaRequest;
import com.kellyacademy.calendar.dto.request.CambiarEstadoTutoriaRequest;
import com.kellyacademy.calendar.dto.request.CrearTutoriaRequest;
import com.kellyacademy.calendar.dto.response.TutoriaResponse;
import com.kellyacademy.calendar.entity.DisponibilidadTutoria;
import com.kellyacademy.calendar.enums.EstadoTutoria;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import com.kellyacademy.user.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

        // Slice #47 (D9): sembrar bloques 00:00-23:59 para todos los dias para docenteDuenoId
        // y docenteAjenoId para que los tests heredados no rompan con TUTORIA_SIN_DISPONIBILIDAD.
        Usuario docenteDueno = usuarioRepository.findById(docenteDuenoId).orElseThrow();
        Usuario docenteAjeno = usuarioRepository.findById(docenteAjenoId).orElseThrow();
        for (DayOfWeek dia : DayOfWeek.values()) {
            DisponibilidadTutoria dDueno = new DisponibilidadTutoria();
            dDueno.setDocente(docenteDueno);
            dDueno.setDiaSemana(dia);
            dDueno.setHoraInicio(LocalTime.MIN);
            dDueno.setHoraFin(LocalTime.of(23, 59));
            dDueno.setBloqueada(false);
            disponibilidadTutoriaRepository.save(dDueno);

            DisponibilidadTutoria dAjeno = new DisponibilidadTutoria();
            dAjeno.setDocente(docenteAjeno);
            dAjeno.setDiaSemana(dia);
            dAjeno.setHoraInicio(LocalTime.MIN);
            dAjeno.setHoraFin(LocalTime.of(23, 59));
            dAjeno.setBloqueada(false);
            disponibilidadTutoriaRepository.save(dAjeno);
        }
    }

    private CrearTutoriaRequest req(UUID estudianteId, UUID docenteId, LocalDateTime fecha) {
        return new CrearTutoriaRequest(estudianteId, docenteId, null, fecha, 60);
    }

    // -------- crear --------

    @Test
    void crear_comoEstudiante_devuelve201() {
        CrearTutoriaRequest req = req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

        ResponseEntity<TutoriaResponse> resp =
                post("/api/tutorias", estudianteToken, req, TutoriaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).estado().name()).isEqualTo("PENDIENTE");
        assertThat(resp.getBody().estudianteId()).isEqualTo(estudianteId);
        assertThat(resp.getBody().docenteId()).isEqualTo(docenteDuenoId);
    }

    @Test
    void crear_comoDocente_devuelve201() {
        CrearTutoriaRequest req = req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

        ResponseEntity<TutoriaResponse> resp =
                post("/api/tutorias", docenteDuenoToken, req, TutoriaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void crear_comoTercero_devuelve403() {
        CrearTutoriaRequest req = req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

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
        CrearTutoriaRequest req = req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

        ResponseEntity<TutoriaResponse> resp =
                post("/api/tutorias", adminToken, req, TutoriaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // -------- listar --------

    @Test
    void listar_comoEstudiante_soloVeLasSuyas() {
        // Tutoria 1: estudiante + docenteDueno. El estudiante participa.
        post("/api/tutorias", estudianteToken,
                req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)),
                TutoriaResponse.class);
        // Tutoria 2: otro estudiante + docenteAjeno. El estudiante autenticado NO participa.
        UUID otroEstudianteId = crearUsuario("Otro", "Est", "otro.est.tut.it@kellyacademy.com", "ESTUDIANTE");
        post("/api/tutorias", docenteAjenoToken,
                req(otroEstudianteId, docenteAjenoId, LocalDateTime.now().plusDays(2).withHour(10).withMinute(0)),
                TutoriaResponse.class);

        ResponseEntity<String> resp = get("/api/tutorias", estudianteToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    @Test
    void listar_comoAdmin_veTodas() {
        post("/api/tutorias", estudianteToken,
                req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)),
                TutoriaResponse.class);
        UUID otroEstudianteId = crearUsuario("Otro", "Est", "otro.est.tut2.it@kellyacademy.com", "ESTUDIANTE");
        post("/api/tutorias", docenteAjenoToken,
                req(otroEstudianteId, docenteAjenoId, LocalDateTime.now().plusDays(2).withHour(10).withMinute(0)),
                TutoriaResponse.class);

        ResponseEntity<String> resp = get("/api/tutorias", adminToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":2");
    }

    @Test
    void listar_filtroPorEstado() {
        post("/api/tutorias", estudianteToken,
                req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)),
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
                        req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)),
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
                        req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)),
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
                        req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)),
                        TutoriaResponse.class).getBody()
        );

        ActualizarTutoriaRequest upd = new ActualizarTutoriaRequest(
                LocalDateTime.now().plusDays(5).withHour(11).withMinute(0), 90, "nota editada"
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
                        req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)),
                        TutoriaResponse.class).getBody()
        );

        ActualizarTutoriaRequest upd = new ActualizarTutoriaRequest(
                LocalDateTime.now().plusDays(5).withHour(10).withMinute(0), 90, null
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
                        req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)),
                        TutoriaResponse.class).getBody()
        );

        ResponseEntity<Void> resp = delete("/api/tutorias/" + creada.id(), adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void eliminar_comoEstudiante_403() {
        TutoriaResponse creada = Objects.requireNonNull(
                post("/api/tutorias", estudianteToken,
                        req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)),
                        TutoriaResponse.class).getBody()
        );

        ResponseEntity<ErrorResponse> resp =
                deleteWithBody("/api/tutorias/" + creada.id(), estudianteToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------
    // cambiarEstado
    // ------------------------------------------------------------------

    private UUID crearTutoriaPendiente() {
        TutoriaResponse creada = Objects.requireNonNull(
                post("/api/tutorias", estudianteToken,
                        req(estudianteId, docenteDuenoId, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)),
                        TutoriaResponse.class).getBody()
        );
        return creada.id();
    }

    private ResponseEntity<TutoriaResponse> patchEstado(
            UUID tutoriaId, EstadoTutoria estado, String token) {
        CambiarEstadoTutoriaRequest body = new CambiarEstadoTutoriaRequest(estado);
        return rest.exchange(
                "/api/tutorias/" + tutoriaId + "/estado",
                org.springframework.http.HttpMethod.PATCH,
                new org.springframework.http.HttpEntity<>(body, headersConToken(token)),
                TutoriaResponse.class
        );
    }

    private ResponseEntity<ErrorResponse> patchEstadoError(
            UUID tutoriaId, EstadoTutoria estado, String token) {
        CambiarEstadoTutoriaRequest body = new CambiarEstadoTutoriaRequest(estado);
        return rest.exchange(
                "/api/tutorias/" + tutoriaId + "/estado",
                org.springframework.http.HttpMethod.PATCH,
                new org.springframework.http.HttpEntity<>(body, headersConToken(token)),
                ErrorResponse.class
        );
    }

    @Test
    void cambiarEstado_pendienteAConfirmada_comoEstudiante_200() {
        UUID tutoriaId = crearTutoriaPendiente();

        ResponseEntity<TutoriaResponse> resp = patchEstado(
                tutoriaId, EstadoTutoria.CONFIRMADA, estudianteToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).estado()).isEqualTo(EstadoTutoria.CONFIRMADA);
    }

    @Test
    void cambiarEstado_pendienteAConfirmada_comoDocente_200() {
        UUID tutoriaId = crearTutoriaPendiente();

        ResponseEntity<TutoriaResponse> resp = patchEstado(
                tutoriaId, EstadoTutoria.CONFIRMADA, docenteDuenoToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).estado()).isEqualTo(EstadoTutoria.CONFIRMADA);
    }

    @Test
    void cambiarEstado_confirmadaACompletada_comoDocente_200() {
        UUID tutoriaId = crearTutoriaPendiente();
        patchEstado(tutoriaId, EstadoTutoria.CONFIRMADA, docenteDuenoToken);

        ResponseEntity<TutoriaResponse> resp = patchEstado(
                tutoriaId, EstadoTutoria.COMPLETADA, docenteDuenoToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).estado()).isEqualTo(EstadoTutoria.COMPLETADA);
    }

    @Test
    void cambiarEstado_confirmadaACompletada_comoEstudiante_403() {
        UUID tutoriaId = crearTutoriaPendiente();
        patchEstado(tutoriaId, EstadoTutoria.CONFIRMADA, docenteDuenoToken);

        ResponseEntity<ErrorResponse> resp = patchEstadoError(
                tutoriaId, EstadoTutoria.COMPLETADA, estudianteToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cambiarEstado_pendienteACancelada_comoEstudiante_200() {
        UUID tutoriaId = crearTutoriaPendiente();

        ResponseEntity<TutoriaResponse> resp = patchEstado(
                tutoriaId, EstadoTutoria.CANCELADA, estudianteToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).estado()).isEqualTo(EstadoTutoria.CANCELADA);
    }

    @Test
    void cambiarEstado_mismoEstado_400() {
        UUID tutoriaId = crearTutoriaPendiente();

        ResponseEntity<ErrorResponse> resp = patchEstadoError(
                tutoriaId, EstadoTutoria.PENDIENTE, estudianteToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("ESTADO_SIN_CAMBIOS");
    }

    @Test
    void cambiarEstado_completadaAConfirmada_400() {
        UUID tutoriaId = crearTutoriaPendiente();
        patchEstado(tutoriaId, EstadoTutoria.CONFIRMADA, docenteDuenoToken);
        patchEstado(tutoriaId, EstadoTutoria.COMPLETADA, docenteDuenoToken);

        ResponseEntity<ErrorResponse> resp = patchEstadoError(
                tutoriaId, EstadoTutoria.CONFIRMADA, docenteDuenoToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }

    @Test
    void cambiarEstado_comoTercero_403() {
        UUID tutoriaId = crearTutoriaPendiente();

        ResponseEntity<ErrorResponse> resp = patchEstadoError(
                tutoriaId, EstadoTutoria.CONFIRMADA, terceroToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cambiarEstado_comoAdmin_200() {
        UUID tutoriaId = crearTutoriaPendiente();

        ResponseEntity<TutoriaResponse> resp = patchEstado(
                tutoriaId, EstadoTutoria.CONFIRMADA, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void cambiarEstado_tutoriaInexistente_404() {
        ResponseEntity<ErrorResponse> resp = patchEstadoError(
                UUID.randomUUID(), EstadoTutoria.CONFIRMADA, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cambiarEstado_sinEstado_400() {
        UUID tutoriaId = crearTutoriaPendiente();

        ResponseEntity<ErrorResponse> resp = rest.exchange(
                "/api/tutorias/" + tutoriaId + "/estado",
                org.springframework.http.HttpMethod.PATCH,
                new org.springframework.http.HttpEntity<>("{}", headersConToken(estudianteToken)),
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------
    // Tests #47 y #48 (Integracion)
    // ------------------------------------------------------------------

    @Test
    void crear_tutoriaSinDisponibilidad_400() {
        UUID docenteSinDispId = crearUsuario("Doc", "SinDisp", "doc.sindisp@kellyacademy.com", "DOCENTE");
        String docenteSinDispToken = login("doc.sindisp@kellyacademy.com", PASSWORD);

        LocalDateTime fecha = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        CrearTutoriaRequest req = req(estudianteId, docenteSinDispId, fecha);

        ResponseEntity<ErrorResponse> resp =
                post("/api/tutorias", docenteSinDispToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("TUTORIA_SIN_DISPONIBILIDAD");
    }

    @Test
    void crear_tutoriaConBloqueBloqueado_400() {
        UUID docenteBloqId = crearUsuario("Doc", "Bloq", "doc.bloq@kellyacademy.com", "DOCENTE");
        Usuario docenteBloq = usuarioRepository.findById(docenteBloqId).orElseThrow();
        String docenteBloqToken = login("doc.bloq@kellyacademy.com", PASSWORD);

        LocalDateTime fecha = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        DisponibilidadTutoria dBloq = new DisponibilidadTutoria();
        dBloq.setDocente(docenteBloq);
        dBloq.setDiaSemana(fecha.getDayOfWeek());
        dBloq.setHoraInicio(LocalTime.of(8, 0));
        dBloq.setHoraFin(LocalTime.of(12, 0));
        dBloq.setBloqueada(true);
        disponibilidadTutoriaRepository.save(dBloq);

        CrearTutoriaRequest req = req(estudianteId, docenteBloqId, fecha);

        ResponseEntity<ErrorResponse> resp =
                post("/api/tutorias", docenteBloqToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("DISPONIBILIDAD_BLOQUEADA");
    }

    @Test
    void crear_tutoriaSolapada_400() {
        LocalDateTime fecha = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);

        // Crear tutoria A y confirmarla
        TutoriaResponse tutoriaA = Objects.requireNonNull(
                post("/api/tutorias", estudianteToken,
                        req(estudianteId, docenteDuenoId, fecha),
                        TutoriaResponse.class).getBody()
        );
        patchEstado(tutoriaA.id(), EstadoTutoria.CONFIRMADA, docenteDuenoToken);

        // Crear tutoria B que solapa (10:30 - 11:30 con A que es 10:00 - 11:00)
        ResponseEntity<ErrorResponse> resp = post("/api/tutorias", estudianteToken,
                req(estudianteId, docenteDuenoId, fecha.plusMinutes(30)),
                ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("TUTORIA_SOLAPADA");
    }

    @Test
    void crear_tutoriaCruzaMedianoche_400() {
        LocalDateTime fechaNoche = LocalDateTime.now().plusDays(1).withHour(23).withMinute(30);

        ResponseEntity<ErrorResponse> resp = post("/api/tutorias", estudianteToken,
                req(estudianteId, docenteDuenoId, fechaNoche),
                ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("TUTORIA_CRUZA_MEDIANOCHE");
    }

    @Test
    void confirmar_tutoriaSolapada_400() {
        LocalDateTime fecha = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0);

        // Tutoria 1: PENDIENTE
        TutoriaResponse tutoria1 = Objects.requireNonNull(
                post("/api/tutorias", estudianteToken,
                        req(estudianteId, docenteDuenoId, fecha),
                        TutoriaResponse.class).getBody()
        );

        // Tutoria 2: PENDIENTE solapada (10:30)
        TutoriaResponse tutoria2 = Objects.requireNonNull(
                post("/api/tutorias", estudianteToken,
                        req(estudianteId, docenteDuenoId, fecha.plusMinutes(30)),
                        TutoriaResponse.class).getBody()
        );

        // Confirmar primera tutoria -> 200
        ResponseEntity<TutoriaResponse> respConf1 = patchEstado(
                tutoria1.id(), EstadoTutoria.CONFIRMADA, docenteDuenoToken);
        assertThat(respConf1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Confirmar segunda tutoria solapada -> 400 TUTORIA_SOLAPADA
        ResponseEntity<ErrorResponse> respConf2 = patchEstadoError(
                tutoria2.id(), EstadoTutoria.CONFIRMADA, docenteDuenoToken);

        assertThat(respConf2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(respConf2.getBody()).getCodigo()).isEqualTo("TUTORIA_SOLAPADA");
    }

    @Test
    void confirmar_tutoriaValida_200() {
        LocalDateTime fecha = LocalDateTime.now().plusDays(4).withHour(10).withMinute(0);

        TutoriaResponse tutoria = Objects.requireNonNull(
                post("/api/tutorias", estudianteToken,
                        req(estudianteId, docenteDuenoId, fecha),
                        TutoriaResponse.class).getBody()
        );

        ResponseEntity<TutoriaResponse> resp = patchEstado(
                tutoria.id(), EstadoTutoria.CONFIRMADA, docenteDuenoToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).estado()).isEqualTo(EstadoTutoria.CONFIRMADA);
    }
}
