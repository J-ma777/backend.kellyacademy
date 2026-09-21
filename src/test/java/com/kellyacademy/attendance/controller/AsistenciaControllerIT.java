package com.kellyacademy.attendance.controller;

import com.kellyacademy.attendance.dto.request.ActualizarAsistenciaRequest;
import com.kellyacademy.attendance.dto.request.CrearAsistenciaRequest;
import com.kellyacademy.attendance.dto.response.AsistenciaResponse;
import com.kellyacademy.attendance.enums.EstadoAsistencia;
import com.kellyacademy.course.dto.request.CrearClaseRequest;
import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.request.CrearSemanaRequest;
import com.kellyacademy.course.dto.request.CrearUnidadRequest;
import com.kellyacademy.course.dto.response.ClaseResponse;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.dto.response.SemanaResponse;
import com.kellyacademy.course.dto.response.UnidadResponse;
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

class AsistenciaControllerIT extends IntegrationTestBase {

    private UUID cursoId;
    private UUID claseId;
    private UUID estudianteId;
    private String estudianteToken;
    private String estudianteAjenoToken;

    @BeforeEach
    void prepararEscenario() {
        estudianteId = crearUsuario("Estudiante", "Propio", "estudiante.asist.it@kellyacademy.com", "ESTUDIANTE");
        crearUsuario("Estudiante", "Ajeno", "estudiante.ajeno.asist.it@kellyacademy.com", "ESTUDIANTE");
        estudianteToken = login("estudiante.asist.it@kellyacademy.com", PASSWORD);
        estudianteAjenoToken = login("estudiante.ajeno.asist.it@kellyacademy.com", PASSWORD);

        // Curso + jerarquia completa via API.
        CrearCursoRequest c = new CrearCursoRequest(
                docenteDuenoId, "Curso Asistencia IT", null, NivelCefr.A2,
                null, LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), 20
        );
        CursoResponse curso = Objects.requireNonNull(
                post("/api/cursos", adminToken, c, CursoResponse.class).getBody()
        );
        cursoId = curso.id();
        activarCurso(cursoId);

        CrearUnidadRequest u = new CrearUnidadRequest(cursoId, 1, "Unidad 1", null);
        UnidadResponse unidad = Objects.requireNonNull(
                post("/api/unidades", docenteDuenoToken, u, UnidadResponse.class).getBody()
        );

        CrearSemanaRequest s = new CrearSemanaRequest(unidad.id(), 1, "Semana 1", null);
        SemanaResponse semana = Objects.requireNonNull(
                post("/api/semanas", docenteDuenoToken, s, SemanaResponse.class).getBody()
        );

        CrearClaseRequest cl = new CrearClaseRequest(
                semana.id(),
                "Clase 1",
                null,
                null,
                null,
                LocalDateTime.now().minusDays(1),
                60,
                "Aula 1"
        );
        ClaseResponse clase = Objects.requireNonNull(
                post("/api/clases", docenteDuenoToken, cl, ClaseResponse.class).getBody()
        );
        claseId = clase.id();

        // Matriculamos al estudiante dueno.
        CrearMatriculaRequest m = new CrearMatriculaRequest(cursoId, estudianteId);
        post("/api/matriculas", adminToken, m, MatriculaResponse.class);
    }

    // -------- crear --------

    @Test
    void crear_comoDocenteDueno_devuelve201() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );

        ResponseEntity<AsistenciaResponse> resp =
                post("/api/asistencias", docenteDuenoToken, req, AsistenciaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).estado()).isEqualTo(EstadoAsistencia.PRESENTE);
        assertThat(resp.getBody().claseId()).isEqualTo(claseId);
        assertThat(resp.getBody().estudianteId()).isEqualTo(estudianteId);
    }

    @Test
    void crear_comoDocenteAjeno_devuelve403() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );

        ResponseEntity<ErrorResponse> resp =
                post("/api/asistencias", docenteAjenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crear_comoEstudiante_devuelve403() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );

        ResponseEntity<ErrorResponse> resp =
                post("/api/asistencias", estudianteToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crear_estudianteNoMatriculado_devuelve400() {
        UUID ajeno = crearUsuario("Est", "NoMat", "est.nomat@kellyacademy.com", "ESTUDIANTE");
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, ajeno, EstadoAsistencia.PRESENTE, null
        );

        ResponseEntity<ErrorResponse> resp =
                post("/api/asistencias", docenteDuenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("ESTUDIANTE_NO_MATRICULADO");
    }

    @Test
    void crear_claseNoImpartida_devuelve400() {
        // Movemos la fecha de la clase al futuro por repositorio:
        // el endpoint de actualizar Clase no expone fechaHora.
        var clase = claseRepository.findById(claseId).orElseThrow();
        clase.setFechaHora(LocalDateTime.now().plusDays(1));
        claseRepository.save(clase);

        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );

        ResponseEntity<ErrorResponse> resp =
                post("/api/asistencias", docenteDuenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("CLASE_NO_IMPARTIDA");
    }

    @Test
    void crear_asistenciaDuplicada_devuelve400() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        post("/api/asistencias", docenteDuenoToken, req, AsistenciaResponse.class);

        ResponseEntity<ErrorResponse> resp =
                post("/api/asistencias", docenteDuenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("ASISTENCIA_DUPLICADA");
    }

    // -------- listar --------

    @Test
    void listar_comoDocenteDueno_devuelve200() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        post("/api/asistencias", docenteDuenoToken, req, AsistenciaResponse.class);

        ResponseEntity<String> resp = get("/api/asistencias?claseId=" + claseId, docenteDuenoToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("content");
    }

    @Test
    void listar_comoDocenteAjeno_devuelveVacio() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        post("/api/asistencias", docenteDuenoToken, req, AsistenciaResponse.class);

        // Docente ajeno no debe ver nada del curso del docente dueno.
        ResponseEntity<String> resp = get("/api/asistencias?claseId=" + claseId, docenteAjenoToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":0");
    }

    @Test
    void listar_comoEstudiante_devuelve403() {
        ResponseEntity<ErrorResponse> resp =
                get("/api/asistencias", estudianteToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------- mis-asistencias --------

    @Test
    void misAsistencias_comoEstudiante_devuelveSusRegistros() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        post("/api/asistencias", docenteDuenoToken, req, AsistenciaResponse.class);

        ResponseEntity<String> resp =
                get("/api/asistencias/mis-asistencias", estudianteToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    // -------- obtener --------

    @Test
    void obtener_comoEstudiantePropio_devuelve200() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        AsistenciaResponse creada = post("/api/asistencias", docenteDuenoToken, req, AsistenciaResponse.class).getBody();
        UUID asistenciaId = Objects.requireNonNull(creada).id();

        ResponseEntity<AsistenciaResponse> resp =
                get("/api/asistencias/" + asistenciaId, estudianteToken, AsistenciaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).id()).isEqualTo(asistenciaId);
    }

    @Test
    void obtener_comoDocenteDueno_devuelve200() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        AsistenciaResponse creada = post("/api/asistencias", docenteDuenoToken, req, AsistenciaResponse.class).getBody();
        UUID asistenciaId = Objects.requireNonNull(creada).id();

        ResponseEntity<AsistenciaResponse> resp =
                get("/api/asistencias/" + asistenciaId, docenteDuenoToken, AsistenciaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void obtener_comoEstudianteAjeno_devuelve403() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        AsistenciaResponse creada = post("/api/asistencias", docenteDuenoToken, req, AsistenciaResponse.class).getBody();
        UUID asistenciaId = Objects.requireNonNull(creada).id();

        ResponseEntity<ErrorResponse> resp =
                get("/api/asistencias/" + asistenciaId, estudianteAjenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------- actualizar --------

    @Test
    void actualizar_comoDocenteDueno_devuelve200() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        AsistenciaResponse creada = post("/api/asistencias", docenteDuenoToken, req, AsistenciaResponse.class).getBody();
        UUID asistenciaId = Objects.requireNonNull(creada).id();

        ActualizarAsistenciaRequest upd = new ActualizarAsistenciaRequest(
                EstadoAsistencia.JUSTIFICADO, "Certificado medico"
        );

        ResponseEntity<AsistenciaResponse> resp =
                put("/api/asistencias/" + asistenciaId, docenteDuenoToken, upd, AsistenciaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).estado()).isEqualTo(EstadoAsistencia.JUSTIFICADO);
        assertThat(resp.getBody().observacion()).isEqualTo("Certificado medico");
    }

    @Test
    void actualizar_comoDocenteAjeno_devuelve403() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        AsistenciaResponse creada = post("/api/asistencias", docenteDuenoToken, req, AsistenciaResponse.class).getBody();
        UUID asistenciaId = Objects.requireNonNull(creada).id();

        ActualizarAsistenciaRequest upd = new ActualizarAsistenciaRequest(
                EstadoAsistencia.AUSENTE, null
        );

        ResponseEntity<ErrorResponse> resp =
                put("/api/asistencias/" + asistenciaId, docenteAjenoToken, upd, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------- eliminar --------

    @Test
    void eliminar_comoAdmin_devuelve204() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        AsistenciaResponse creada = post("/api/asistencias", docenteDuenoToken, req, AsistenciaResponse.class).getBody();
        UUID asistenciaId = Objects.requireNonNull(creada).id();

        ResponseEntity<Void> resp = delete("/api/asistencias/" + asistenciaId, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void eliminar_comoDocente_devuelve403() {
        CrearAsistenciaRequest req = new CrearAsistenciaRequest(
                claseId, estudianteId, EstadoAsistencia.PRESENTE, null
        );
        AsistenciaResponse creada = post("/api/asistencias", docenteDuenoToken, req, AsistenciaResponse.class).getBody();
        UUID asistenciaId = Objects.requireNonNull(creada).id();

        ResponseEntity<ErrorResponse> resp =
                deleteWithBody("/api/asistencias/" + asistenciaId, docenteDuenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}