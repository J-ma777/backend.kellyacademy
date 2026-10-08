package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.CambiarEstadoCursoRequest;
import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CursoControllerIT extends IntegrationTestBase {

    private UUID crearCursoEnBorrador() {
        CrearCursoRequest req = new CrearCursoRequest(
                docenteDuenoId, "Curso Estado IT", "Descripcion",
                NivelCefr.B1, "Lunes 19:00",
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), 30
        );
        ResponseEntity<CursoResponse> resp = post("/api/cursos", adminToken, req, CursoResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().estado()).isEqualTo(EstadoCurso.BORRADOR);
        return resp.getBody().id();
    }

    private ResponseEntity<CursoResponse> patchEstado(UUID cursoId, EstadoCurso nuevo, String token) {
        CambiarEstadoCursoRequest req = new CambiarEstadoCursoRequest(nuevo);
        return rest.exchange(
                "/api/cursos/" + cursoId + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>(req, headersConToken(token)),
                CursoResponse.class
        );
    }

    private ResponseEntity<ErrorResponse> patchEstadoError(UUID cursoId, EstadoCurso nuevo, String token) {
        CambiarEstadoCursoRequest req = new CambiarEstadoCursoRequest(nuevo);
        return rest.exchange(
                "/api/cursos/" + cursoId + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>(req, headersConToken(token)),
                ErrorResponse.class
        );
    }

    @Test
    void cambiarEstado_borradorAActivo_comoAdmin_devuelve200() {
        UUID cursoId = crearCursoEnBorrador();

        ResponseEntity<CursoResponse> resp = patchEstado(cursoId, EstadoCurso.ACTIVO, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().estado()).isEqualTo(EstadoCurso.ACTIVO);
    }

    @Test
    void cambiarEstado_borradorAArchivado_comoAdmin_devuelve200() {
        UUID cursoId = crearCursoEnBorrador();

        ResponseEntity<CursoResponse> resp = patchEstado(cursoId, EstadoCurso.ARCHIVADO, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().estado()).isEqualTo(EstadoCurso.ARCHIVADO);
    }

    @Test
    void cambiarEstado_activoAFinalizado_devuelve200() {
        UUID cursoId = crearCursoEnBorrador();
        patchEstado(cursoId, EstadoCurso.ACTIVO, adminToken);

        ResponseEntity<CursoResponse> resp = patchEstado(cursoId, EstadoCurso.FINALIZADO, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().estado()).isEqualTo(EstadoCurso.FINALIZADO);
    }

    @Test
    void cambiarEstado_finalizadoAArchivado_devuelve200() {
        UUID cursoId = crearCursoEnBorrador();
        patchEstado(cursoId, EstadoCurso.ACTIVO, adminToken);
        patchEstado(cursoId, EstadoCurso.FINALIZADO, adminToken);

        ResponseEntity<CursoResponse> resp = patchEstado(cursoId, EstadoCurso.ARCHIVADO, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().estado()).isEqualTo(EstadoCurso.ARCHIVADO);
    }

    @Test
    void cambiarEstado_mismoEstado_devuelve400() {
        UUID cursoId = crearCursoEnBorrador();

        ResponseEntity<ErrorResponse> resp = patchEstadoError(cursoId, EstadoCurso.BORRADOR, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("ESTADO_SIN_CAMBIOS");
    }

    @Test
    void cambiarEstado_archivadoAActivo_devuelve400() {
        UUID cursoId = crearCursoEnBorrador();
        patchEstado(cursoId, EstadoCurso.ARCHIVADO, adminToken);

        ResponseEntity<ErrorResponse> resp = patchEstadoError(cursoId, EstadoCurso.ACTIVO, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }

    @Test
    void cambiarEstado_activoABorrador_devuelve400() {
        UUID cursoId = crearCursoEnBorrador();
        patchEstado(cursoId, EstadoCurso.ACTIVO, adminToken);

        ResponseEntity<ErrorResponse> resp = patchEstadoError(cursoId, EstadoCurso.BORRADOR, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }

    @Test
    void cambiarEstado_borradorAFinalizado_devuelve400() {
        UUID cursoId = crearCursoEnBorrador();

        ResponseEntity<ErrorResponse> resp = patchEstadoError(cursoId, EstadoCurso.FINALIZADO, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }

    @Test
    void cambiarEstado_comoDocenteDueno_devuelve403() {
        UUID cursoId = crearCursoEnBorrador();

        ResponseEntity<ErrorResponse> resp = rest.exchange(
                "/api/cursos/" + cursoId + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>(new CambiarEstadoCursoRequest(EstadoCurso.ACTIVO),
                        headersConToken(docenteDuenoToken)),
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cambiarEstado_cursoInexistente_devuelve404() {
        ResponseEntity<ErrorResponse> resp = patchEstadoError(
                UUID.randomUUID(), EstadoCurso.ACTIVO, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cambiarEstado_sinEstado_devuelve400() {
        UUID cursoId = crearCursoEnBorrador();

        ResponseEntity<ErrorResponse> resp = rest.exchange(
                "/api/cursos/" + cursoId + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>("{}", headersConToken(adminToken)),
                ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void eliminar_cuandoActivo_400() {
        UUID cursoId = crearCursoEnBorrador();
        patchEstado(cursoId, EstadoCurso.ACTIVO, adminToken);

        ResponseEntity<ErrorResponse> resp = deleteWithBody(
                "/api/cursos/" + cursoId, adminToken, ErrorResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo("CURSO_NO_ELIMINABLE");
    }

    @Test
    void eliminar_cuandoBorrador_204() {
        UUID cursoId = crearCursoEnBorrador();

        ResponseEntity<Void> resp = delete("/api/cursos/" + cursoId, adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}