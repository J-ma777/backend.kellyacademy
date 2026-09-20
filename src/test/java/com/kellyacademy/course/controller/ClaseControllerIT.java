package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.CrearClaseRequest;
import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.request.CrearSemanaRequest;
import com.kellyacademy.course.dto.request.CrearUnidadRequest;
import com.kellyacademy.course.dto.response.ClaseResponse;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.dto.response.SemanaResponse;
import com.kellyacademy.course.dto.response.UnidadResponse;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClaseControllerIT extends IntegrationTestBase {

    private UUID semanaId;

    @BeforeEach
    void prepararJerarquia() {
        CrearCursoRequest c = new CrearCursoRequest(
                docenteDuenoId, "Curso IT", null, NivelCefr.B1,
                null, LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), 30
        );
        ResponseEntity<CursoResponse> curso = post("/api/cursos", adminToken, c, CursoResponse.class);

        CrearUnidadRequest u = new CrearUnidadRequest(curso.getBody().id(), 1, "U1", null);
        ResponseEntity<UnidadResponse> unidad = post("/api/unidades", adminToken, u, UnidadResponse.class);

        CrearSemanaRequest s = new CrearSemanaRequest(unidad.getBody().id(), 1, "S1", null);
        ResponseEntity<SemanaResponse> semana = post("/api/semanas", adminToken, s, SemanaResponse.class);
        semanaId = semana.getBody().id();
    }

    @Test
    void crear_comoAdmin_devuelve201() {
        CrearClaseRequest req = new CrearClaseRequest(
                semanaId, "Clase 1", "Desc",
                "https://meet.example.com/a", null, null, 60, "Sala 1"
        );

        ResponseEntity<ClaseResponse> resp = post("/api/clases", adminToken, req, ClaseResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().semanaId()).isEqualTo(semanaId);
    }

    @Test
    void crear_comoDocenteDueno_devuelve201() {
        CrearClaseRequest req = new CrearClaseRequest(
                semanaId, "Clase 1", "Desc",
                "https://meet.example.com/a", null, null, 60, "Sala 1"
        );

        ResponseEntity<ClaseResponse> resp = post("/api/clases", docenteDuenoToken, req, ClaseResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void crear_comoDocenteAjeno_devuelve403() {
        CrearClaseRequest req = new CrearClaseRequest(
                semanaId, "Clase 1", "Desc",
                "https://meet.example.com/a", null, null, 60, "Sala 1"
        );

        ResponseEntity<ErrorResponse> resp = post("/api/clases", docenteAjenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crear_urlInvalida_devuelve400() {
        CrearClaseRequest req = new CrearClaseRequest(
                semanaId, "Clase 1", "Desc",
                "no-es-url", null, null, 60, null
        );

        ResponseEntity<ErrorResponse> resp = post("/api/clases", adminToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("URL_INVALIDA");
    }

    @Test
    void listar_sinSemanaId_devuelve400() {
        ResponseEntity<ErrorResponse> resp = get("/api/clases", adminToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("SEMANA_ID_REQUERIDO");
    }

    @Test
    void eliminar_comoAdmin_devuelve204() {
        CrearClaseRequest req = new CrearClaseRequest(
                semanaId, "Clase 1", "Desc",
                "https://meet.example.com/a", null, null, 60, null
        );
        UUID claseId = post("/api/clases", adminToken, req, ClaseResponse.class).getBody().id();

        ResponseEntity<Void> resp = delete("/api/clases/" + claseId, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}