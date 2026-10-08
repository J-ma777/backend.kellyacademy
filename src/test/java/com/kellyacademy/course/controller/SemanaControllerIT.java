package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.request.CrearSemanaRequest;
import com.kellyacademy.course.dto.request.CrearUnidadRequest;
import com.kellyacademy.course.dto.request.ReordenarRequest;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.dto.response.SemanaResponse;
import com.kellyacademy.course.dto.response.SemanaResumenResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemanaControllerIT extends IntegrationTestBase {

    private UUID unidadId;

    @BeforeEach
    void prepararCursoYUnidad() {
        CrearCursoRequest c = new CrearCursoRequest(
                docenteDuenoId, "Curso IT", null, NivelCefr.B1,
                null, LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), 30
        );
        ResponseEntity<CursoResponse> curso = post("/api/cursos", adminToken, c, CursoResponse.class);

        CrearUnidadRequest u = new CrearUnidadRequest(curso.getBody().id(), 1, "U1", null);
        ResponseEntity<UnidadResponse> unidad = post("/api/unidades", adminToken, u, UnidadResponse.class);
        unidadId = unidad.getBody().id();
    }

    @Test
    void crear_comoAdmin_devuelve201_conEsActualFalse() {
        CrearSemanaRequest req = new CrearSemanaRequest(unidadId, 1, "S1", null);

        ResponseEntity<SemanaResponse> resp = post("/api/semanas", adminToken, req, SemanaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().esActual()).isFalse();
    }

    @Test
    void crear_numeroDuplicado_devuelve400() {
        CrearSemanaRequest req = new CrearSemanaRequest(unidadId, 1, "S1", null);
        post("/api/semanas", adminToken, req, SemanaResponse.class);

        ResponseEntity<ErrorResponse> resp = post("/api/semanas", adminToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("NUMERO_DUPLICADO");
    }

    @Test
    void marcarActual_desmarcaLaAnterior() {
        UUID s1 = post("/api/semanas", adminToken, new CrearSemanaRequest(unidadId, 1, "S1", null), SemanaResponse.class).getBody().id();
        UUID s2 = post("/api/semanas", adminToken, new CrearSemanaRequest(unidadId, 2, "S2", null), SemanaResponse.class).getBody().id();

        // Marcar S1.
        ResponseEntity<SemanaResponse> r1 = patch("/api/semanas/" + s1 + "/marcar-actual", adminToken, SemanaResponse.class);
        assertThat(r1.getBody().esActual()).isTrue();

        // Marcar S2: S1 debe quedar en false.
        ResponseEntity<SemanaResponse> r2 = patch("/api/semanas/" + s2 + "/marcar-actual", adminToken, SemanaResponse.class);
        assertThat(r2.getBody().esActual()).isTrue();

        ResponseEntity<SemanaResponse> s1Despues = get("/api/semanas/" + s1, adminToken, SemanaResponse.class);
        assertThat(s1Despues.getBody().esActual()).isFalse();
    }

    @Test
    void marcarActual_yaEsActual_esIdempotente() {
        UUID s1 = post("/api/semanas", adminToken, new CrearSemanaRequest(unidadId, 1, "S1", null), SemanaResponse.class).getBody().id();

        patch("/api/semanas/" + s1 + "/marcar-actual", adminToken, SemanaResponse.class);
        ResponseEntity<SemanaResponse> resp = patch("/api/semanas/" + s1 + "/marcar-actual", adminToken, SemanaResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().esActual()).isTrue();
    }

    @Test
    void marcarActual_comoDocenteAjeno_devuelve403() {
        UUID s1 = post("/api/semanas", adminToken, new CrearSemanaRequest(unidadId, 1, "S1", null), SemanaResponse.class).getBody().id();

        ResponseEntity<ErrorResponse> resp = patch("/api/semanas/" + s1 + "/marcar-actual", docenteAjenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void eliminar_semanaVacia_devuelve204() {
        UUID s1 = post("/api/semanas", adminToken, new CrearSemanaRequest(unidadId, 1, "S1", null), SemanaResponse.class).getBody().id();

        ResponseEntity<Void> resp = delete("/api/semanas/" + s1, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

        @Test
        void reordenar_comoDocenteDueno_devuelve200() {
        SemanaResponse s1 = post("/api/semanas", adminToken,
            new CrearSemanaRequest(unidadId, 1, "S1", null), SemanaResponse.class).getBody();
        SemanaResponse s2 = post("/api/semanas", adminToken,
            new CrearSemanaRequest(unidadId, 2, "S2", null), SemanaResponse.class).getBody();

        ResponseEntity<SemanaResumenResponse[]> resp = put(
            "/api/unidades/" + unidadId + "/semanas/reordenar", docenteDuenoToken,
            new ReordenarRequest(List.of(
                new ReordenarRequest.ItemOrden(s2.id(), 1),
                new ReordenarRequest.ItemOrden(s1.id(), 2)
            )), SemanaResumenResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()[0].id()).isEqualTo(s2.id());
        }

        @Test
        void reordenar_comoAdmin_devuelve200() {
        SemanaResponse s1 = post("/api/semanas", adminToken,
            new CrearSemanaRequest(unidadId, 1, "S1", null), SemanaResponse.class).getBody();
        SemanaResponse s2 = post("/api/semanas", adminToken,
            new CrearSemanaRequest(unidadId, 2, "S2", null), SemanaResponse.class).getBody();

        ResponseEntity<SemanaResumenResponse[]> resp = put(
            "/api/unidades/" + unidadId + "/semanas/reordenar", adminToken,
            new ReordenarRequest(List.of(
                new ReordenarRequest.ItemOrden(s2.id(), 1),
                new ReordenarRequest.ItemOrden(s1.id(), 2)
            )), SemanaResumenResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void reordenar_comoDocenteAjeno_devuelve403() {
        ResponseEntity<ErrorResponse> resp = put(
            "/api/unidades/" + unidadId + "/semanas/reordenar", docenteAjenoToken,
            new ReordenarRequest(List.of(new ReordenarRequest.ItemOrden(UUID.randomUUID(), 1))),
            ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void reordenar_faltanIds_devuelve400() {
        SemanaResponse s1 = post("/api/semanas", adminToken,
            new CrearSemanaRequest(unidadId, 1, "S1", null), SemanaResponse.class).getBody();
        post("/api/semanas", adminToken, new CrearSemanaRequest(unidadId, 2, "S2", null), SemanaResponse.class);

        ResponseEntity<ErrorResponse> resp = put(
            "/api/unidades/" + unidadId + "/semanas/reordenar", adminToken,
            new ReordenarRequest(List.of(new ReordenarRequest.ItemOrden(s1.id(), 1))), ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("ORDEN_INCOMPLETO");
        }

        @Test
        void reordenar_idsAjenos_devuelve400() {
        SemanaResponse s1 = post("/api/semanas", adminToken,
            new CrearSemanaRequest(unidadId, 1, "S1", null), SemanaResponse.class).getBody();
        SemanaResponse s2 = post("/api/semanas", adminToken,
            new CrearSemanaRequest(unidadId, 2, "S2", null), SemanaResponse.class).getBody();

        ResponseEntity<ErrorResponse> resp = put(
            "/api/unidades/" + unidadId + "/semanas/reordenar", adminToken,
            new ReordenarRequest(List.of(
                new ReordenarRequest.ItemOrden(s1.id(), 1),
                new ReordenarRequest.ItemOrden(s2.id(), 2),
                new ReordenarRequest.ItemOrden(UUID.randomUUID(), 3)
            )), ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("ORDEN_CON_IDS_AJENOS");
        }

        @Test
        void reordenar_numerosDuplicados_devuelve400() {
        SemanaResponse s1 = post("/api/semanas", adminToken,
            new CrearSemanaRequest(unidadId, 1, "S1", null), SemanaResponse.class).getBody();
        SemanaResponse s2 = post("/api/semanas", adminToken,
            new CrearSemanaRequest(unidadId, 2, "S2", null), SemanaResponse.class).getBody();

        ResponseEntity<ErrorResponse> resp = put(
            "/api/unidades/" + unidadId + "/semanas/reordenar", adminToken,
            new ReordenarRequest(List.of(
                new ReordenarRequest.ItemOrden(s1.id(), 1),
                new ReordenarRequest.ItemOrden(s2.id(), 1)
            )), ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("ORDEN_CON_NUMEROS_DUPLICADOS");
        }
}