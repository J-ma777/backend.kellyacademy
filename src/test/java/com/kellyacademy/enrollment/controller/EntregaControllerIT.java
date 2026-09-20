package com.kellyacademy.enrollment.controller;

import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.request.CrearSemanaRequest;
import com.kellyacademy.course.dto.request.CrearTareaRequest;
import com.kellyacademy.course.dto.request.CrearUnidadRequest;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.dto.response.SemanaResponse;
import com.kellyacademy.course.dto.response.TareaResponse;
import com.kellyacademy.course.dto.response.UnidadResponse;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.enrollment.dto.request.ActualizarEntregaRequest;
import com.kellyacademy.enrollment.dto.request.CrearEntregaRequest;
import com.kellyacademy.enrollment.dto.request.CrearMatriculaRequest;
import com.kellyacademy.enrollment.dto.response.EntregaResponse;
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

class EntregaControllerIT extends IntegrationTestBase {

    private UUID tareaId;
    private UUID estudianteId;
    private String estudianteToken;

    @BeforeEach
    void prepararEscenario() {
        // Estudiante propio + token.
        estudianteId = crearUsuario("Estudiante", "Entrega", "estudiante.entrega@kellyacademy.com", "ESTUDIANTE");
        estudianteToken = login("estudiante.entrega@kellyacademy.com", PASSWORD);

        // Curso del docente dueno, activo, con capacidad.
        CrearCursoRequest c = new CrearCursoRequest(
                docenteDuenoId, "Curso Entrega IT", null, NivelCefr.B1,
                null, LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), 30
        );
        CursoResponse curso = post("/api/cursos", adminToken, c, CursoResponse.class).getBody();
        UUID cursoId = Objects.requireNonNull(curso).id();
        activarCurso(cursoId);

        // Matricula del estudiante en el curso.
        post("/api/matriculas", adminToken,
                new CrearMatriculaRequest(cursoId, estudianteId),
                MatriculaResponse.class);

        // Jerarquia: unidad -> semana -> tarea con deadline futuro.
        UUID unidadId = Objects.requireNonNull(
                post("/api/unidades", adminToken,
                        new CrearUnidadRequest(cursoId, 1, "U1", null),
                        UnidadResponse.class).getBody()).id();

        UUID semanaId = Objects.requireNonNull(
                post("/api/semanas", adminToken,
                        new CrearSemanaRequest(unidadId, 1, "S1", null),
                        SemanaResponse.class).getBody()).id();

        CrearTareaRequest tareaReq = new CrearTareaRequest(
                semanaId, "Tarea 1", "Descripcion",
                null, LocalDateTime.now().plusDays(7), 100
        );
        tareaId = Objects.requireNonNull(
                post("/api/tareas", docenteDuenoToken, tareaReq, TareaResponse.class).getBody()).id();
    }

    @Test
    void crear_comoDocenteDueno_devuelve201() {
        CrearEntregaRequest req = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );

        ResponseEntity<EntregaResponse> resp = post("/api/entregas", docenteDuenoToken, req, EntregaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).tareaId()).isEqualTo(tareaId);
        assertThat(resp.getBody().estudianteId()).isEqualTo(estudianteId);
    }

    @Test
    void crear_comoDocenteAjeno_devuelve403() {
        CrearEntregaRequest req = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );

        ResponseEntity<ErrorResponse> resp = post("/api/entregas", docenteAjenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crear_comoEstudiante_devuelve403() {
        CrearEntregaRequest req = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );

        ResponseEntity<ErrorResponse> resp = post("/api/entregas", estudianteToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crear_urlInvalida_devuelve400() {
        CrearEntregaRequest req = new CrearEntregaRequest(
                tareaId, estudianteId, "no-es-url"
        );

        ResponseEntity<ErrorResponse> resp = post("/api/entregas", docenteDuenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("URL_INVALIDA");
    }

    @Test
    void crear_entregaDuplicada_devuelve400() {
        CrearEntregaRequest req = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        post("/api/entregas", docenteDuenoToken, req, EntregaResponse.class);

        ResponseEntity<ErrorResponse> resp = post("/api/entregas", docenteDuenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("ENTREGA_DUPLICADA");
    }

    @Test
    void actualizar_comoEstudiantePropio_devuelve200() {
        CrearEntregaRequest crearReq = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        UUID entregaId = Objects.requireNonNull(
                post("/api/entregas", docenteDuenoToken, crearReq, EntregaResponse.class).getBody()).id();

        ActualizarEntregaRequest actReq = new ActualizarEntregaRequest("https://example.com/nuevo.pdf");
        ResponseEntity<EntregaResponse> resp =
                put("/api/entregas/" + entregaId, estudianteToken, actReq, EntregaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).urlArchivo()).isEqualTo("https://example.com/nuevo.pdf");
    }

    @Test
    void obtener_comoEstudiantePropio_devuelve200() {
        CrearEntregaRequest req = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        UUID entregaId = Objects.requireNonNull(
                post("/api/entregas", docenteDuenoToken, req, EntregaResponse.class).getBody()).id();

        ResponseEntity<EntregaResponse> resp =
                get("/api/entregas/" + entregaId, estudianteToken, EntregaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void misEntregas_devuelveSoloLasPropias() {
        CrearEntregaRequest req = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        post("/api/entregas", docenteDuenoToken, req, EntregaResponse.class);

        ResponseEntity<String> resp = get("/api/entregas/mis-entregas", estudianteToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("content");
    }

    @Test
    void eliminar_comoAdmin_devuelve204() {
        CrearEntregaRequest req = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        UUID entregaId = Objects.requireNonNull(
                post("/api/entregas", docenteDuenoToken, req, EntregaResponse.class).getBody()).id();

        ResponseEntity<Void> resp = delete("/api/entregas/" + entregaId, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}