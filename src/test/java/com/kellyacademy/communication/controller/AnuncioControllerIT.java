package com.kellyacademy.communication.controller;

import com.kellyacademy.communication.dto.request.ActualizarAnuncioRequest;
import com.kellyacademy.communication.dto.request.CrearAnuncioRequest;
import com.kellyacademy.communication.dto.response.AnuncioResponse;
import com.kellyacademy.communication.repository.NotificacionRepository;
import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.enrollment.dto.request.CrearMatriculaRequest;
import com.kellyacademy.enrollment.dto.response.MatriculaResponse;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnuncioControllerIT extends IntegrationTestBase {

    @Autowired private NotificacionRepository notificacionRepository;

    private UUID cursoId;
    private UUID estudianteId;
    private String estudianteToken;

    @BeforeEach
    void prepararEscenario() {
        estudianteId = crearUsuario("Est", "Propio", "est.anuncio.it@kellyacademy.com", "ESTUDIANTE");
        estudianteToken = login("est.anuncio.it@kellyacademy.com", PASSWORD);

        CrearCursoRequest c = new CrearCursoRequest(
                docenteDuenoId, "Curso Anuncio IT", null, NivelCefr.A2,
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

    // -------- crear --------

    @Test
    void crear_comoDocenteDueno_devuelve201YNotificaEstudiantes() {
        CrearAnuncioRequest req = new CrearAnuncioRequest(cursoId, "Titulo", "Cuerpo");

        ResponseEntity<AnuncioResponse> resp =
                post("/api/anuncios", docenteDuenoToken, req, AnuncioResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).titulo()).isEqualTo("Titulo");
        assertThat(resp.getBody().activo()).isTrue();

        // El estudiante matriculado debe tener 1 notificacion.
        assertThat(notificacionRepository.countByUsuarioIdAndLeidaFalse(estudianteId)).isEqualTo(1L);
    }

    @Test
    void crear_comoDocenteAjeno_devuelve403() {
        CrearAnuncioRequest req = new CrearAnuncioRequest(cursoId, "Titulo", "Cuerpo");

        ResponseEntity<ErrorResponse> resp =
                post("/api/anuncios", docenteAjenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crear_comoEstudiante_devuelve403() {
        CrearAnuncioRequest req = new CrearAnuncioRequest(cursoId, "Titulo", "Cuerpo");

        ResponseEntity<ErrorResponse> resp =
                post("/api/anuncios", estudianteToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------- listar --------

    @Test
    void listar_comoAutenticado_devuelvePage() {
        CrearAnuncioRequest req = new CrearAnuncioRequest(cursoId, "Titulo", "Cuerpo");
        post("/api/anuncios", docenteDuenoToken, req, AnuncioResponse.class);

        ResponseEntity<String> resp = get(
                "/api/anuncios?cursoId=" + cursoId, estudianteToken, String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("content");
    }

    // -------- obtener --------

    @Test
    void obtener_cuandoExiste_devuelve200() {
        CrearAnuncioRequest req = new CrearAnuncioRequest(cursoId, "Titulo", "Cuerpo");
        AnuncioResponse creado = post("/api/anuncios", docenteDuenoToken, req, AnuncioResponse.class).getBody();
        UUID id = Objects.requireNonNull(creado).id();

        ResponseEntity<AnuncioResponse> resp = get("/api/anuncios/" + id, estudianteToken, AnuncioResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).id()).isEqualTo(id);
    }

    @Test
    void obtener_cuandoNoExiste_devuelve404() {
        ResponseEntity<ErrorResponse> resp = get(
                "/api/anuncios/" + UUID.randomUUID(), estudianteToken, ErrorResponse.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // -------- actualizar --------

    @Test
    void actualizar_comoDocenteDueno_devuelve200() {
        CrearAnuncioRequest req = new CrearAnuncioRequest(cursoId, "Titulo", "Cuerpo");
        AnuncioResponse creado = post("/api/anuncios", docenteDuenoToken, req, AnuncioResponse.class).getBody();
        UUID id = Objects.requireNonNull(creado).id();

        ActualizarAnuncioRequest upd = new ActualizarAnuncioRequest("Nuevo titulo", "Nuevo cuerpo");
        ResponseEntity<AnuncioResponse> resp =
                put("/api/anuncios/" + id, docenteDuenoToken, upd, AnuncioResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).titulo()).isEqualTo("Nuevo titulo");
    }

    // -------- archivar --------

    @Test
    void archivar_comoDocenteDueno_devuelve200YActivoFalse() {
        CrearAnuncioRequest req = new CrearAnuncioRequest(cursoId, "Titulo", "Cuerpo");
        AnuncioResponse creado = post("/api/anuncios", docenteDuenoToken, req, AnuncioResponse.class).getBody();
        UUID id = Objects.requireNonNull(creado).id();

        ResponseEntity<AnuncioResponse> resp =
                patch("/api/anuncios/" + id + "/archivar", docenteDuenoToken, AnuncioResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).activo()).isFalse();
    }

    // -------- eliminar --------

    @Test
    void eliminar_comoDocenteDueno_devuelve204() {
        CrearAnuncioRequest req = new CrearAnuncioRequest(cursoId, "Titulo", "Cuerpo");
        AnuncioResponse creado = post("/api/anuncios", docenteDuenoToken, req, AnuncioResponse.class).getBody();
        UUID id = Objects.requireNonNull(creado).id();

        ResponseEntity<Void> resp = delete("/api/anuncios/" + id, docenteDuenoToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void eliminar_comoDocenteAjeno_devuelve403() {
        CrearAnuncioRequest req = new CrearAnuncioRequest(cursoId, "Titulo", "Cuerpo");
        AnuncioResponse creado = post("/api/anuncios", docenteDuenoToken, req, AnuncioResponse.class).getBody();
        UUID id = Objects.requireNonNull(creado).id();

        ResponseEntity<ErrorResponse> resp =
                deleteWithBody("/api/anuncios/" + id, docenteAjenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}