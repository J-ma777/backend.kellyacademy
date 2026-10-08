package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.request.CrearSemanaRequest;
import com.kellyacademy.course.dto.request.CrearUnidadRequest;
import com.kellyacademy.course.dto.request.ReordenarRequest;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.dto.response.UnidadResponse;
import com.kellyacademy.course.dto.response.UnidadResumenResponse;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnidadControllerIT extends IntegrationTestBase {

    private UUID cursoId;

    @BeforeEach
    void crearCursoParaTests() {
        CrearCursoRequest req = new CrearCursoRequest(
                docenteDuenoId, "Curso IT", "Descripcion IT", NivelCefr.B1,
                "Lunes 19:00", LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), 30
        );
        ResponseEntity<CursoResponse> resp = post("/api/cursos", adminToken, req, CursoResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        cursoId = resp.getBody().id();
    }

    @Test
    void crear_comoAdmin_devuelve201() {
        CrearUnidadRequest req = new CrearUnidadRequest(cursoId, 1, "Unidad 1", "Desc");

        ResponseEntity<UnidadResponse> resp = post("/api/unidades", adminToken, req, UnidadResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().cursoId()).isEqualTo(cursoId);
        assertThat(resp.getBody().numero()).isEqualTo(1);
    }

    @Test
    void crear_comoDocenteDueno_devuelve201() {
        CrearUnidadRequest req = new CrearUnidadRequest(cursoId, 1, "Unidad 1", "Desc");

        ResponseEntity<UnidadResponse> resp = post("/api/unidades", docenteDuenoToken, req, UnidadResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void crear_comoDocenteAjeno_devuelve403() {
        CrearUnidadRequest req = new CrearUnidadRequest(cursoId, 1, "Unidad 1", "Desc");

        ResponseEntity<ErrorResponse> resp = post("/api/unidades", docenteAjenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().getCodigo()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void crear_numeroDuplicado_devuelve400() {
        CrearUnidadRequest req = new CrearUnidadRequest(cursoId, 1, "Unidad 1", "Desc");
        post("/api/unidades", adminToken, req, UnidadResponse.class);

        ResponseEntity<ErrorResponse> resp = post("/api/unidades", adminToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("NUMERO_DUPLICADO");
    }

    @Test
    void listar_porCurso_devuelveUnidadesOrdenadasPorNumero() {
        post("/api/unidades", adminToken, new CrearUnidadRequest(cursoId, 2, "U2", null), UnidadResponse.class);
        post("/api/unidades", adminToken, new CrearUnidadRequest(cursoId, 1, "U1", null), UnidadResponse.class);

        ResponseEntity<UnidadResumenResponse[]> resp = rest.exchange(
                "/api/unidades?cursoId=" + cursoId,
                HttpMethod.GET,
                new HttpEntity<>(headersConToken(adminToken)),
                UnidadResumenResponse[].class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
        assertThat(resp.getBody()[0].numero()).isEqualTo(1);
        assertThat(resp.getBody()[1].numero()).isEqualTo(2);
    }

    @Test
    void listar_sinCursoId_devuelve400() {
        ResponseEntity<ErrorResponse> resp = get("/api/unidades", adminToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("CURSO_ID_REQUERIDO");
    }

    @Test
    void eliminar_unidadConSemanas_devuelve400() {
        ResponseEntity<UnidadResponse> creada = post("/api/unidades", adminToken,
                new CrearUnidadRequest(cursoId, 1, "U1", null), UnidadResponse.class);
        UUID unidadId = creada.getBody().id();

        post("/api/semanas", adminToken, new CrearSemanaRequest(unidadId, 1, "S1", null), Object.class);

        ResponseEntity<ErrorResponse> resp = deleteWithBody("/api/unidades/" + unidadId, adminToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("UNIDAD_CON_SEMANAS");
    }

        @Test
        void reordenar_comoDocenteDueno_devuelve200() {
        UnidadResponse u1 = post("/api/unidades", adminToken,
            new CrearUnidadRequest(cursoId, 1, "U1", null), UnidadResponse.class).getBody();
        UnidadResponse u2 = post("/api/unidades", adminToken,
            new CrearUnidadRequest(cursoId, 2, "U2", null), UnidadResponse.class).getBody();

        ResponseEntity<UnidadResumenResponse[]> resp = put(
            "/api/cursos/" + cursoId + "/unidades/reordenar", docenteDuenoToken,
            new ReordenarRequest(List.of(
                new ReordenarRequest.ItemOrden(u2.id(), 1),
                new ReordenarRequest.ItemOrden(u1.id(), 2)
            )), UnidadResumenResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()[0].id()).isEqualTo(u2.id());
        }

        @Test
        void reordenar_comoAdmin_devuelve200() {
        UnidadResponse u1 = post("/api/unidades", adminToken,
            new CrearUnidadRequest(cursoId, 1, "U1", null), UnidadResponse.class).getBody();
        UnidadResponse u2 = post("/api/unidades", adminToken,
            new CrearUnidadRequest(cursoId, 2, "U2", null), UnidadResponse.class).getBody();

        ResponseEntity<UnidadResumenResponse[]> resp = put(
            "/api/cursos/" + cursoId + "/unidades/reordenar", adminToken,
            new ReordenarRequest(List.of(
                new ReordenarRequest.ItemOrden(u2.id(), 1),
                new ReordenarRequest.ItemOrden(u1.id(), 2)
            )), UnidadResumenResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void reordenar_comoDocenteAjeno_devuelve403() {
        ResponseEntity<ErrorResponse> resp = put(
            "/api/cursos/" + cursoId + "/unidades/reordenar", docenteAjenoToken,
            new ReordenarRequest(List.of(new ReordenarRequest.ItemOrden(UUID.randomUUID(), 1))),
            ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void reordenar_ordenVacio_devuelve400() {
        ResponseEntity<ErrorResponse> resp = put(
            "/api/cursos/" + cursoId + "/unidades/reordenar", adminToken,
            new ReordenarRequest(List.of()), ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void reordenar_faltanIds_devuelve400() {
        UnidadResponse u1 = post("/api/unidades", adminToken,
            new CrearUnidadRequest(cursoId, 1, "U1", null), UnidadResponse.class).getBody();
        post("/api/unidades", adminToken, new CrearUnidadRequest(cursoId, 2, "U2", null), UnidadResponse.class);

        ResponseEntity<ErrorResponse> resp = put(
            "/api/cursos/" + cursoId + "/unidades/reordenar", adminToken,
            new ReordenarRequest(List.of(new ReordenarRequest.ItemOrden(u1.id(), 1))), ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("ORDEN_INCOMPLETO");
        }

        @Test
        void reordenar_idsAjenos_devuelve400() {
        UnidadResponse u1 = post("/api/unidades", adminToken,
            new CrearUnidadRequest(cursoId, 1, "U1", null), UnidadResponse.class).getBody();
        UnidadResponse u2 = post("/api/unidades", adminToken,
            new CrearUnidadRequest(cursoId, 2, "U2", null), UnidadResponse.class).getBody();

        ResponseEntity<ErrorResponse> resp = put(
            "/api/cursos/" + cursoId + "/unidades/reordenar", adminToken,
            new ReordenarRequest(List.of(
                new ReordenarRequest.ItemOrden(u1.id(), 1),
                new ReordenarRequest.ItemOrden(u2.id(), 2),
                new ReordenarRequest.ItemOrden(UUID.randomUUID(), 3)
            )), ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("ORDEN_CON_IDS_AJENOS");
        }

        @Test
        void reordenar_numerosDuplicados_devuelve400() {
        UnidadResponse u1 = post("/api/unidades", adminToken,
            new CrearUnidadRequest(cursoId, 1, "U1", null), UnidadResponse.class).getBody();
        UnidadResponse u2 = post("/api/unidades", adminToken,
            new CrearUnidadRequest(cursoId, 2, "U2", null), UnidadResponse.class).getBody();

        ResponseEntity<ErrorResponse> resp = put(
            "/api/cursos/" + cursoId + "/unidades/reordenar", adminToken,
            new ReordenarRequest(List.of(
                new ReordenarRequest.ItemOrden(u1.id(), 1),
                new ReordenarRequest.ItemOrden(u2.id(), 1)
            )), ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("ORDEN_CON_NUMEROS_DUPLICADOS");
        }
}