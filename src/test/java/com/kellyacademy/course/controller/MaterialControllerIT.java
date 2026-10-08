package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.request.CrearMaterialRequest;
import com.kellyacademy.course.dto.request.CrearSemanaRequest;
import com.kellyacademy.course.dto.request.CrearUnidadRequest;
import com.kellyacademy.course.dto.request.CambiarSemanaRequest;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.dto.response.MaterialResponse;
import com.kellyacademy.course.dto.response.SemanaResponse;
import com.kellyacademy.course.dto.response.UnidadResponse;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.enums.TipoMaterial;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialControllerIT extends IntegrationTestBase {

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
    void crear_sinUrlArchivoNiUrlExterno_devuelve400() {
        CrearMaterialRequest req = new CrearMaterialRequest(
                semanaId, "M1", "Desc", null, TipoMaterial.PDF, null, null
        );

        ResponseEntity<ErrorResponse> resp = post("/api/materiales", adminToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("MATERIAL_SIN_URL");
    }

    @Test
    void crear_conUrlArchivo_devuelve201() {
        CrearMaterialRequest req = new CrearMaterialRequest(
                semanaId, "M1", "Desc",
                "https://cdn.example.com/file.pdf", TipoMaterial.PDF, null, null
        );

        ResponseEntity<MaterialResponse> resp = post("/api/materiales", adminToken, req, MaterialResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void crear_conUrlExternaInvalida_devuelve400() {
        CrearMaterialRequest req = new CrearMaterialRequest(
                semanaId, "M1", "Desc",
                null, TipoMaterial.ENLACE, null, "no-es-url"
        );

        ResponseEntity<ErrorResponse> resp = post("/api/materiales", adminToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("URL_INVALIDA");
    }

    @Test
    void crear_comoDocenteAjeno_devuelve403() {
        CrearMaterialRequest req = new CrearMaterialRequest(
                semanaId, "M1", "Desc",
                "https://cdn.example.com/file.pdf", TipoMaterial.PDF, null, null
        );

        ResponseEntity<ErrorResponse> resp = post("/api/materiales", docenteAjenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listar_sinSemanaId_devuelve400() {
        ResponseEntity<ErrorResponse> resp = get("/api/materiales", adminToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("SEMANA_ID_REQUERIDO");
    }

        @Test
        void cambiarSemana_comoDocenteDueno_devuelve200() {
        UUID materialId = post("/api/materiales", adminToken,
            new CrearMaterialRequest(semanaId, "M1", "Desc", "https://cdn.example.com/a.pdf", TipoMaterial.PDF, null, null),
            MaterialResponse.class).getBody().id();

        ResponseEntity<MaterialResponse> resp = patch("/api/materiales/" + materialId + "/semana", docenteDuenoToken,
            new CambiarSemanaRequest(segundaSemanaId), MaterialResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().semanaId()).isEqualTo(segundaSemanaId);
        }

        @Test
        void cambiarSemana_comoDocenteAjeno_devuelve403() {
        UUID materialId = post("/api/materiales", adminToken,
            new CrearMaterialRequest(semanaId, "M1", "Desc", "https://cdn.example.com/a.pdf", TipoMaterial.PDF, null, null),
            MaterialResponse.class).getBody().id();

        ResponseEntity<ErrorResponse> resp = patch("/api/materiales/" + materialId + "/semana", docenteAjenoToken,
            new CambiarSemanaRequest(segundaSemanaId), ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void cambiarSemana_mismaSemana_devuelve400() {
        UUID materialId = post("/api/materiales", adminToken,
            new CrearMaterialRequest(semanaId, "M1", "Desc", "https://cdn.example.com/a.pdf", TipoMaterial.PDF, null, null),
            MaterialResponse.class).getBody().id();

        ResponseEntity<ErrorResponse> resp = patch("/api/materiales/" + materialId + "/semana", adminToken,
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
        UUID materialId = post("/api/materiales", adminToken,
            new CrearMaterialRequest(semanaId, "M1", "Desc", "https://cdn.example.com/a.pdf", TipoMaterial.PDF, null, null),
            MaterialResponse.class).getBody().id();

        ResponseEntity<ErrorResponse> resp = patch("/api/materiales/" + materialId + "/semana", adminToken,
            new CambiarSemanaRequest(otraSemanaId), ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("SEMANA_FUERA_DE_CURSO");
        }
}