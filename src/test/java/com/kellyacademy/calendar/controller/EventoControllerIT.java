package com.kellyacademy.calendar.controller;

import com.kellyacademy.calendar.dto.request.ActualizarEventoRequest;
import com.kellyacademy.calendar.dto.request.CrearEventoRequest;
import com.kellyacademy.calendar.dto.response.EventoResponse;
import com.kellyacademy.calendar.enums.TipoEvento;
import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.enrollment.dto.request.CrearMatriculaRequest;
import com.kellyacademy.enrollment.dto.response.MatriculaResponse;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventoControllerIT extends IntegrationTestBase {

    private UUID cursoId;
    private UUID estudianteId;
    private String estudianteToken;

    @BeforeEach
    void prepararEscenario() {
        // Estudiante matriculado en el curso (se usa en test de "pertenencia por estudiante").
        estudianteId = crearUsuario("Estudiante", "Evento", "est.evento.it@kellyacademy.com", "ESTUDIANTE");
        estudianteToken = login("est.evento.it@kellyacademy.com", PASSWORD);

        CrearCursoRequest c = new CrearCursoRequest(
                docenteDuenoId, "Curso Evento IT", null, NivelCefr.A2,
                null, LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), 20
        );
        CursoResponse curso = Objects.requireNonNull(
                post("/api/cursos", adminToken, c, CursoResponse.class).getBody()
        );
        cursoId = curso.id();
        activarCurso(cursoId);

        CrearMatriculaRequest m = new CrearMatriculaRequest(cursoId, estudianteId);
        post("/api/matriculas", adminToken, m, MatriculaResponse.class);
    }

    private CrearEventoRequest reqEvento(UUID cursoId, LocalDateTime inicio, LocalDateTime fin) {
        return new CrearEventoRequest(cursoId, "Evento IT", "desc", TipoEvento.EXAMEN, inicio, fin, "Aula 1");
    }

    // -------- crear --------

    @Test
    void crear_sinCurso_devuelve201() {
        CrearEventoRequest req = reqEvento(null,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));

        ResponseEntity<EventoResponse> resp =
                post("/api/eventos", docenteDuenoToken, req, EventoResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).usuarioId()).isEqualTo(docenteDuenoId);
        assertThat(resp.getBody().cursoId()).isNull();
    }

    @Test
    void crear_finAntesDeInicio_devuelve400() {
        CrearEventoRequest req = reqEvento(null,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1));

        ResponseEntity<ErrorResponse> resp =
                post("/api/eventos", docenteDuenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("FECHAS_INVALIDAS");
    }

    @Test
    void crear_conCurso_docenteDueno_devuelve201() {
        CrearEventoRequest req = reqEvento(cursoId, LocalDateTime.now().plusDays(1), null);

        ResponseEntity<EventoResponse> resp =
                post("/api/eventos", docenteDuenoToken, req, EventoResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).cursoId()).isEqualTo(cursoId);
    }

    @Test
    void crear_conCurso_estudianteMatriculado_devuelve201() {
        CrearEventoRequest req = reqEvento(cursoId, LocalDateTime.now().plusDays(1), null);

        ResponseEntity<EventoResponse> resp =
                post("/api/eventos", estudianteToken, req, EventoResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).cursoId()).isEqualTo(cursoId);
        assertThat(resp.getBody().usuarioId()).isEqualTo(estudianteId);
    }

    @Test
    void crear_conCurso_usuarioNoPertenece_devuelve400() {
        // docenteAjenoId no es dueno del curso ni esta matriculado.
        CrearEventoRequest req = reqEvento(cursoId, LocalDateTime.now().plusDays(1), null);

        ResponseEntity<ErrorResponse> resp =
                post("/api/eventos", docenteAjenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo())
                .isEqualTo("USUARIO_NO_PERTENECE_AL_CURSO");
    }

    // -------- listar --------

    @Test
    void listar_propios_noVeOtros() {
        post("/api/eventos", docenteDuenoToken,
                reqEvento(null, LocalDateTime.now().plusDays(1), null), EventoResponse.class);
        post("/api/eventos", docenteAjenoToken,
                reqEvento(null, LocalDateTime.now().plusDays(1), null), EventoResponse.class);

        ResponseEntity<String> resp = get("/api/eventos", docenteDuenoToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    @Test
    void listar_adminPuedeFiltrarPorUsuario() {
        post("/api/eventos", docenteDuenoToken,
                reqEvento(null, LocalDateTime.now().plusDays(1), null), EventoResponse.class);
        post("/api/eventos", docenteAjenoToken,
                reqEvento(null, LocalDateTime.now().plusDays(1), null), EventoResponse.class);

        ResponseEntity<String> resp =
                get("/api/eventos?usuarioId=" + docenteAjenoId, adminToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    // -------- obtener --------

    @Test
    void obtener_ajeno_devuelve403() {
        EventoResponse creado = Objects.requireNonNull(
                post("/api/eventos", docenteDuenoToken,
                        reqEvento(null, LocalDateTime.now().plusDays(1), null),
                        EventoResponse.class).getBody()
        );

        ResponseEntity<ErrorResponse> resp =
                get("/api/eventos/" + creado.id(), docenteAjenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void obtener_admin_devuelve200() {
        EventoResponse creado = Objects.requireNonNull(
                post("/api/eventos", docenteDuenoToken,
                        reqEvento(null, LocalDateTime.now().plusDays(1), null),
                        EventoResponse.class).getBody()
        );

        ResponseEntity<EventoResponse> resp =
                get("/api/eventos/" + creado.id(), adminToken, EventoResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // -------- actualizar --------

    @Test
    void actualizar_propio_devuelve200() {
        EventoResponse creado = Objects.requireNonNull(
                post("/api/eventos", docenteDuenoToken,
                        reqEvento(null, LocalDateTime.now().plusDays(1), null),
                        EventoResponse.class).getBody()
        );

        ActualizarEventoRequest req = new ActualizarEventoRequest(
                "Nuevo titulo", "nueva desc", TipoEvento.REUNION,
                LocalDateTime.now().plusDays(3), null, "Aula 2"
        );

        ResponseEntity<EventoResponse> resp =
                put("/api/eventos/" + creado.id(), docenteDuenoToken, req, EventoResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).titulo()).isEqualTo("Nuevo titulo");
        assertThat(resp.getBody().tipo()).isEqualTo(TipoEvento.REUNION);
        assertThat(resp.getBody().cursoId()).isNull();
    }

    // -------- eliminar --------

    @Test
    void eliminar_propio_devuelve204() {
        EventoResponse creado = Objects.requireNonNull(
                post("/api/eventos", docenteDuenoToken,
                        reqEvento(null, LocalDateTime.now().plusDays(1), null),
                        EventoResponse.class).getBody()
        );

        ResponseEntity<Void> resp = delete("/api/eventos/" + creado.id(), docenteDuenoToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void eliminar_ajeno_devuelve403() {
        EventoResponse creado = Objects.requireNonNull(
                post("/api/eventos", docenteDuenoToken,
                        reqEvento(null, LocalDateTime.now().plusDays(1), null),
                        EventoResponse.class).getBody()
        );

        ResponseEntity<ErrorResponse> resp =
                deleteWithBody("/api/eventos/" + creado.id(), docenteAjenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}