package com.kellyacademy.enrollment.controller;

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
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatriculaControllerIT extends IntegrationTestBase {

    private UUID cursoId;
    private UUID estudianteId;
    private String estudianteToken;
    private String estudianteAjenoToken;

    @BeforeEach
    void prepararEscenario() {
        // Estudiantes creados por test (IntegrationTestBase no los siembra).
        estudianteId = crearUsuario("Estudiante", "Propio", "estudiante.it@kellyacademy.com", "ESTUDIANTE");
        crearUsuario("Estudiante", "Ajeno", "estudiante.ajeno.it@kellyacademy.com", "ESTUDIANTE");
        estudianteToken = login("estudiante.it@kellyacademy.com", PASSWORD);
        estudianteAjenoToken = login("estudiante.ajeno.it@kellyacademy.com", PASSWORD);

        // Curso creado via API (nace en BORRADOR).
        CrearCursoRequest c = new CrearCursoRequest(
                docenteDuenoId, "Curso Matricula IT", null, NivelCefr.A2,
                null, LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), 5
        );
        ResponseEntity<CursoResponse> curso = post("/api/cursos", adminToken, c, CursoResponse.class);
        cursoId = Objects.requireNonNull(curso.getBody()).id();

        // Activamos el curso por repositorio (no hay endpoint de cambio de estado aun: deuda #13, FASE 5).
        activarCurso(cursoId);
    }

    @Test
    void crear_comoAdmin_devuelve201() {
        CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, estudianteId);

        ResponseEntity<MatriculaResponse> resp = post("/api/matriculas", adminToken, req, MatriculaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).cursoId()).isEqualTo(cursoId);
        assertThat(resp.getBody().estudianteId()).isEqualTo(estudianteId);
        assertThat(resp.getBody().estado().name()).isEqualTo("ACTIVA");
    }

    @Test
    void crear_comoDocente_devuelve403() {
        CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, estudianteId);

        ResponseEntity<ErrorResponse> resp = post("/api/matriculas", docenteDuenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crear_comoEstudiante_devuelve403() {
        CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, estudianteId);

        ResponseEntity<ErrorResponse> resp = post("/api/matriculas", estudianteToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crear_cursoNoActivo_devuelve400() {
        desactivarCurso(cursoId);
        CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, estudianteId);

        ResponseEntity<ErrorResponse> resp = post("/api/matriculas", adminToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("CURSO_NO_ACTIVO");
    }

    @Test
    void crear_cursoSinCupo_devuelve400() {
        // Capacidad del curso es 5. Matriculamos 5 estudiantes distintos para llenar.
        for (int i = 0; i < 5; i++) {
            UUID estId = crearUsuario("Est" + i, "Test", "est" + i + ".cupo@kellyacademy.com", "ESTUDIANTE");
            CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, estId);
            post("/api/matriculas", adminToken, req, MatriculaResponse.class);
        }

        // El sexto debe fallar.
        UUID sexto = crearUsuario("Est", "Sexto", "est.sexto@kellyacademy.com", "ESTUDIANTE");
        CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, sexto);

        ResponseEntity<ErrorResponse> resp = post("/api/matriculas", adminToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("CURSO_SIN_CUPO");
    }

    @Test
    void crear_matriculaDuplicada_devuelve400() {
        CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, estudianteId);
        post("/api/matriculas", adminToken, req, MatriculaResponse.class);

        ResponseEntity<ErrorResponse> resp = post("/api/matriculas", adminToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("MATRICULA_DUPLICADA");
    }

    @Test
    void crear_estudianteSinRolEstudiante_devuelve400() {
        // El docente dueno tiene rol DOCENTE, no ESTUDIANTE.
        CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, docenteDuenoId);

        ResponseEntity<ErrorResponse> resp = post("/api/matriculas", adminToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("ESTUDIANTE_SIN_ROL");
    }

    @Test
    void listar_comoAdmin_devuelvePage() {
        CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, estudianteId);
        post("/api/matriculas", adminToken, req, MatriculaResponse.class);

        ResponseEntity<String> resp = get("/api/matriculas", adminToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("content");
    }

    @Test
    void listar_comoDocente_devuelve403() {
        ResponseEntity<ErrorResponse> resp = get("/api/matriculas", docenteDuenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void obtener_comoEstudiantePropio_devuelve200() {
        CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, estudianteId);
        MatriculaResponse creada = post("/api/matriculas", adminToken, req, MatriculaResponse.class).getBody();
        UUID matriculaId = Objects.requireNonNull(creada).id();

        ResponseEntity<MatriculaResponse> resp =
                get("/api/matriculas/" + matriculaId, estudianteToken, MatriculaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).id()).isEqualTo(matriculaId);
    }

    @Test
    void obtener_comoDocenteDueno_devuelve200() {
        CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, estudianteId);
        MatriculaResponse creada = post("/api/matriculas", adminToken, req, MatriculaResponse.class).getBody();
        UUID matriculaId = Objects.requireNonNull(creada).id();

        ResponseEntity<MatriculaResponse> resp =
                get("/api/matriculas/" + matriculaId, docenteDuenoToken, MatriculaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void obtener_comoEstudianteAjeno_devuelve403() {
        CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, estudianteId);
        MatriculaResponse creada = post("/api/matriculas", adminToken, req, MatriculaResponse.class).getBody();
        UUID matriculaId = Objects.requireNonNull(creada).id();

        ResponseEntity<ErrorResponse> resp =
                get("/api/matriculas/" + matriculaId, estudianteAjenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void eliminar_comoAdmin_devuelve204() {
        CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, estudianteId);
        MatriculaResponse creada = post("/api/matriculas", adminToken, req, MatriculaResponse.class).getBody();
        UUID matriculaId = Objects.requireNonNull(creada).id();

        ResponseEntity<Void> resp = delete("/api/matriculas/" + matriculaId, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void eliminar_comoDocente_devuelve403() {
        CrearMatriculaRequest req = new CrearMatriculaRequest(cursoId, estudianteId);
        MatriculaResponse creada = post("/api/matriculas", adminToken, req, MatriculaResponse.class).getBody();
        UUID matriculaId = Objects.requireNonNull(creada).id();

        ResponseEntity<ErrorResponse> resp =
                deleteWithBody("/api/matriculas/" + matriculaId, docenteDuenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}