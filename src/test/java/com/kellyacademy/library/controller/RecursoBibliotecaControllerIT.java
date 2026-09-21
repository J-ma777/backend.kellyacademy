package com.kellyacademy.library.controller;

import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.enums.TipoMaterial;
import com.kellyacademy.library.dto.request.ActualizarRecursoRequest;
import com.kellyacademy.library.dto.request.CrearRecursoRequest;
import com.kellyacademy.library.dto.response.RecursoResponse;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecursoBibliotecaControllerIT extends IntegrationTestBase {

    private String estudianteToken;

    @BeforeEach
    void prepararEscenario() {
        crearUsuario("Est", "Rec", "est.recurso.it@kellyacademy.com", "ESTUDIANTE");
        estudianteToken = login("est.recurso.it@kellyacademy.com", PASSWORD);
    }

    private CrearRecursoRequest req(String urlArchivo, String urlExterno) {
        return new CrearRecursoRequest(
                "Listening A2",
                "descripcion",
                "Listening",
                NivelCefr.A2,
                TipoMaterial.AUDIO,
                urlArchivo,
                urlExterno,
                null
        );
    }

    // -------- crear --------

    @Test
    void crear_comoDocente_201() {
        CrearRecursoRequest req = req("https://cdn.kelly.com/audio.mp3", null);

        ResponseEntity<RecursoResponse> resp =
                post("/api/recursos", docenteDuenoToken, req, RecursoResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(resp.getBody()).titulo()).isEqualTo("Listening A2");
        assertThat(resp.getBody().contadorDescargas()).isZero();
    }

    @Test
    void crear_comoAdmin_201() {
        CrearRecursoRequest req = req(null, "https://youtube.com/watch?v=xyz");

        ResponseEntity<RecursoResponse> resp =
                post("/api/recursos", adminToken, req, RecursoResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void crear_comoEstudiante_403() {
        CrearRecursoRequest req = req("https://cdn.kelly.com/audio.mp3", null);

        ResponseEntity<ErrorResponse> resp =
                post("/api/recursos", estudianteToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void crear_sinUrl_400() {
        CrearRecursoRequest req = req(null, null);

        ResponseEntity<ErrorResponse> resp =
                post("/api/recursos", docenteDuenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("RECURSO_SIN_URL");
    }

    @Test
    void crear_urlInvalida_400() {
        CrearRecursoRequest req = req("no-es-url", null);

        ResponseEntity<ErrorResponse> resp =
                post("/api/recursos", docenteDuenoToken, req, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("URL_INVALIDA");
    }

    // -------- listar --------

    @Test
    void listar_comoEstudiante_200() {
        post("/api/recursos", docenteDuenoToken,
                req("https://cdn.kelly.com/audio.mp3", null), RecursoResponse.class);

        ResponseEntity<String> resp = get("/api/recursos", estudianteToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    @Test
    void listar_filtroPorTipo() {
        post("/api/recursos", docenteDuenoToken,
                req("https://cdn.kelly.com/audio.mp3", null), RecursoResponse.class);
        post("/api/recursos", docenteDuenoToken,
                req(null, "https://youtube.com/watch?v=xyz"), RecursoResponse.class);

        ResponseEntity<String> resp =
                get("/api/recursos?tipo=AUDIO", docenteDuenoToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":2");
    }

    @Test
    void listar_filtroPorTitulo() {
        post("/api/recursos", docenteDuenoToken,
                req("https://cdn.kelly.com/audio.mp3", null), RecursoResponse.class);

        ResponseEntity<String> resp =
                get("/api/recursos?titulo=Listening", docenteDuenoToken, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\":1");
    }

    // -------- obtener --------

    @Test
    void obtener_comoEstudiante_200() {
        RecursoResponse creado = Objects.requireNonNull(
                post("/api/recursos", docenteDuenoToken,
                        req("https://cdn.kelly.com/audio.mp3", null),
                        RecursoResponse.class).getBody()
        );

        ResponseEntity<RecursoResponse> resp =
                get("/api/recursos/" + creado.id(), estudianteToken, RecursoResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).id()).isEqualTo(creado.id());
    }

    @Test
    void obtener_inexistente_404() {
        ResponseEntity<ErrorResponse> resp =
                get("/api/recursos/" + UUID.randomUUID(), docenteDuenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // -------- actualizar --------

    @Test
    void actualizar_comoDocente_200() {
        RecursoResponse creado = Objects.requireNonNull(
                post("/api/recursos", docenteDuenoToken,
                        req("https://cdn.kelly.com/audio.mp3", null),
                        RecursoResponse.class).getBody()
        );

        ActualizarRecursoRequest upd = new ActualizarRecursoRequest(
                "Nuevo titulo", "nueva desc", "Grammar", NivelCefr.B1,
                TipoMaterial.PDF, "https://cdn.kelly.com/new.pdf", null, null
        );

        ResponseEntity<RecursoResponse> resp =
                put("/api/recursos/" + creado.id(), docenteDuenoToken, upd, RecursoResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(resp.getBody()).titulo()).isEqualTo("Nuevo titulo");
    }

    @Test
    void actualizar_comoEstudiante_403() {
        RecursoResponse creado = Objects.requireNonNull(
                post("/api/recursos", docenteDuenoToken,
                        req("https://cdn.kelly.com/audio.mp3", null),
                        RecursoResponse.class).getBody()
        );

        ActualizarRecursoRequest upd = new ActualizarRecursoRequest(
                "X", null, null, null, TipoMaterial.PDF,
                "https://cdn.kelly.com/x.pdf", null, null
        );

        ResponseEntity<ErrorResponse> resp =
                put("/api/recursos/" + creado.id(), estudianteToken, upd, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void actualizar_sinUrl_400() {
        RecursoResponse creado = Objects.requireNonNull(
                post("/api/recursos", docenteDuenoToken,
                        req("https://cdn.kelly.com/audio.mp3", null),
                        RecursoResponse.class).getBody()
        );

        ActualizarRecursoRequest upd = new ActualizarRecursoRequest(
                "X", null, null, null, TipoMaterial.PDF, null, null, null
        );

        ResponseEntity<ErrorResponse> resp =
                put("/api/recursos/" + creado.id(), docenteDuenoToken, upd, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).getCodigo()).isEqualTo("RECURSO_SIN_URL");
    }

    // -------- eliminar --------

    @Test
    void eliminar_comoAdmin_204() {
        RecursoResponse creado = Objects.requireNonNull(
                post("/api/recursos", docenteDuenoToken,
                        req("https://cdn.kelly.com/audio.mp3", null),
                        RecursoResponse.class).getBody()
        );

        ResponseEntity<Void> resp = delete("/api/recursos/" + creado.id(), adminToken);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void eliminar_comoDocente_403() {
        RecursoResponse creado = Objects.requireNonNull(
                post("/api/recursos", docenteDuenoToken,
                        req("https://cdn.kelly.com/audio.mp3", null),
                        RecursoResponse.class).getBody()
        );

        ResponseEntity<ErrorResponse> resp =
                deleteWithBody("/api/recursos/" + creado.id(), docenteDuenoToken, ErrorResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}