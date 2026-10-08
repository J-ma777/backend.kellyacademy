package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.request.CrearSemanaRequest;
import com.kellyacademy.course.dto.request.CrearTareaRequest;
import com.kellyacademy.course.dto.request.CrearUnidadRequest;
import com.kellyacademy.course.dto.request.CambiarSemanaRequest;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.dto.response.SemanaResponse;
import com.kellyacademy.course.dto.response.TareaResponse;
import com.kellyacademy.course.dto.response.UnidadResponse;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TareaControllerIT extends IntegrationTestBase {

    private UUID semanaId;
    private UUID segundaSemanaId;

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
        segundaSemanaId = post("/api/semanas", adminToken,
            new CrearSemanaRequest(unidad.getBody().id(), 2, "S2", null), SemanaResponse.class).getBody().id();
    }

    @Test
    void crear_comoAdmin_devuelve201() {
        CrearTareaRequest req = new CrearTareaRequest(
                semanaId, "T1", "Desc",
                "https://instrucciones.example.com/a",
                LocalDateTime.now().plusDays(7), 100
        );

        ResponseEntity<TareaResponse> resp = post("/api/tareas", adminToken, req, TareaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().semanaId()).isEqualTo(semanaId);
    }

    @Test
    void crear_comoDocenteDueno_devuelve201() {
        CrearTareaRequest req = new CrearTareaRequest(
                semanaId, "T1", "Desc", null,
                LocalDateTime.now().plusDays(7), 100
        );

        ResponseEntity<TareaResponse> resp = post("/api/tareas", docenteDuenoToken, req, TareaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void crear_instruccionesUrlInvalida_devuelve400() {
        CrearTareaRequest req = new CrearTareaRequest(
                semanaId, "T1", "Desc",
                "no-es-url", LocalDateTime.now().plusDays(7), 100
        );

        ResponseEntity<ErrorResponse> resp = post("/api/tareas", adminToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("URL_INVALIDA");
    }

    @Test
    void crear_comoDocenteAjeno_devuelve403() {
        CrearTareaRequest req = new CrearTareaRequest(
                semanaId, "T1", "Desc", null,
                LocalDateTime.now().plusDays(7), 100
        );

        ResponseEntity<ErrorResponse> resp = post("/api/tareas", docenteAjenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listar_sinSemanaId_devuelve400() {
        ResponseEntity<ErrorResponse> resp = get("/api/tareas", adminToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("SEMANA_ID_REQUERIDO");
    }

        @Test
        void cambiarSemana_comoDocenteDueno_devuelve200() {
        UUID tareaId = post("/api/tareas", adminToken,
            new CrearTareaRequest(semanaId, "T1", "Desc", null, LocalDateTime.now().plusDays(7), 100),
            TareaResponse.class).getBody().id();

        ResponseEntity<TareaResponse> resp = patch("/api/tareas/" + tareaId + "/semana", docenteDuenoToken,
            new CambiarSemanaRequest(segundaSemanaId), TareaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().semanaId()).isEqualTo(segundaSemanaId);
        }

        @Test
        void cambiarSemana_comoDocenteAjeno_devuelve403() {
        UUID tareaId = post("/api/tareas", adminToken,
            new CrearTareaRequest(semanaId, "T1", "Desc", null, LocalDateTime.now().plusDays(7), 100),
            TareaResponse.class).getBody().id();

        ResponseEntity<ErrorResponse> resp = patch("/api/tareas/" + tareaId + "/semana", docenteAjenoToken,
            new CambiarSemanaRequest(segundaSemanaId), ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void cambiarSemana_mismaSemana_devuelve400() {
        UUID tareaId = post("/api/tareas", adminToken,
            new CrearTareaRequest(semanaId, "T1", "Desc", null, LocalDateTime.now().plusDays(7), 100),
            TareaResponse.class).getBody().id();

        ResponseEntity<ErrorResponse> resp = patch("/api/tareas/" + tareaId + "/semana", adminToken,
            new CambiarSemanaRequest(semanaId), ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("SEMANA_SIN_CAMBIOS");
        }

        @Test
        void cambiarSemana_semanaFueraDeCurso_devuelve400() {
        CrearCursoRequest curso = new CrearCursoRequest(
            docenteDuenoId, "Otro curso", null, NivelCefr.B1, null,
            LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), 30);
        UUID otroCursoId = post("/api/cursos", adminToken, curso, CursoResponse.class).getBody().id();
        UUID otraUnidadId = post("/api/unidades", adminToken,
            new CrearUnidadRequest(otroCursoId, 1, "Otra unidad", null), UnidadResponse.class).getBody().id();
        UUID otraSemanaId = post("/api/semanas", adminToken,
            new CrearSemanaRequest(otraUnidadId, 1, "Otra semana", null), SemanaResponse.class).getBody().id();
        UUID tareaId = post("/api/tareas", adminToken,
            new CrearTareaRequest(semanaId, "T1", "Desc", null, LocalDateTime.now().plusDays(7), 100),
            TareaResponse.class).getBody().id();

        ResponseEntity<ErrorResponse> resp = patch("/api/tareas/" + tareaId + "/semana", adminToken,
            new CambiarSemanaRequest(otraSemanaId), ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("SEMANA_FUERA_DE_CURSO");
        }
}