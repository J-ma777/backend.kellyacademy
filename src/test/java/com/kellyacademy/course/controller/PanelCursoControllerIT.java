package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.response.*;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.enrollment.dto.response.EntregaResponse;
import com.kellyacademy.enrollment.dto.response.MatriculaResponse;
import com.kellyacademy.communication.dto.response.AnuncioResponse;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class PanelCursoControllerIT extends IntegrationTestBase {
    private CursoResponse crearCurso(UUID docenteId, String titulo) {
        var response = post("/api/cursos", adminToken,
                new CrearCursoRequest(docenteId, titulo, "Descripción del curso", NivelCefr.B1,
                        null, null, null, 20), CursoResponse.class);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return response.getBody();
    }

    @Test
    void panel_reflejaContenidoPersistidoAnunciosAlumnosYNotaPerfecta() {
        var curso = crearCurso(docenteDuenoId, "Curso del panel");
        var vacio = get("/api/cursos/" + curso.id() + "/panel-docente", docenteDuenoToken, PanelCursoResponse.class);
        assertThat(vacio.getBody().unidades()).isEmpty();
        UUID estudianteId = crearUsuario("Alumna", "Panel", "alumna.panel@it.invalid", "ESTUDIANTE");
        var unidad = post("/api/unidades", docenteDuenoToken,
                Map.of("cursoId", curso.id(), "numero", 1, "titulo", "Unidad real"), UnidadResponse.class).getBody();
        var semana = post("/api/semanas", docenteDuenoToken,
                Map.of("unidadId", unidad.id(), "numero", 1, "titulo", "Semana real"), SemanaResponse.class).getBody();
        var material = post("/api/materiales", docenteDuenoToken,
                Map.of("semanaId", semana.id(), "titulo", "Lectura", "tipo", "PDF", "urlExterno", "https://example.org/lectura"), MaterialResponse.class).getBody();
        var clase = post("/api/clases", docenteDuenoToken,
                Map.of("semanaId", semana.id(), "titulo", "Clase programada", "fechaHora", LocalDateTime.now().plusDays(1).toString(),
                        "duracionMinutos", 45, "urlVivo", "https://example.org/sesion"), ClaseResponse.class).getBody();
        var tarea = post("/api/tareas", docenteDuenoToken,
                Map.of("semanaId", semana.id(), "titulo", "Evaluación", "puntajeMaximo", 1000), TareaResponse.class).getBody();
        post("/api/matriculas", adminToken, Map.of("cursoId", curso.id(), "estudianteId", estudianteId), MatriculaResponse.class);
        var anuncio = post("/api/anuncios", docenteDuenoToken,
                Map.of("cursoId", curso.id(), "titulo", "Aviso", "cuerpo", "Instrucciones reales"), AnuncioResponse.class).getBody();
        var entrega = post("/api/entregas", docenteDuenoToken,
                Map.of("tareaId", tarea.id(), "estudianteId", estudianteId, "urlArchivo", "https://example.org/entrega"), EntregaResponse.class).getBody();
        var calificada = patch("/api/entregas/" + entrega.id() + "/calificar", docenteDuenoToken,
                Map.of("nota", 1000, "retroalimentacion", "Trabajo completo"), EntregaResponse.class);
        assertThat(calificada.getStatusCode().value()).isEqualTo(200);
        put("/api/materiales/" + material.id(), docenteDuenoToken,
                Map.of("titulo", "Lectura actualizada", "tipo", "PDF", "urlExterno", "https://example.org/lectura-nueva"), MaterialResponse.class);
        patch("/api/semanas/" + semana.id() + "/marcar-actual", docenteDuenoToken, SemanaResponse.class);

        // Otro curso no debe contaminar las listas del panel.
        crearCurso(docenteAjenoId, "Curso ajeno");
        var response = get("/api/cursos/" + curso.id() + "/panel-docente", docenteDuenoToken, PanelCursoResponse.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var panel = response.getBody();
        assertThat(panel.curso().id()).isEqualTo(curso.id());
        assertThat(panel.unidades()).hasSize(1);
        var contenido = panel.unidades().getFirst().semanas().getFirst();
        assertThat(contenido.semana().esActual()).isTrue();
        assertThat(contenido.materiales()).singleElement().satisfies(m -> {
            assertThat(m.titulo()).isEqualTo("Lectura actualizada");
            assertThat(m.urlExterno()).isEqualTo("https://example.org/lectura-nueva");
        });
        assertThat(contenido.clases()).singleElement().satisfies(c -> {
            assertThat(c.id()).isEqualTo(clase.id());
            assertThat(c.duracionMinutos()).isEqualTo(45);
        });
        assertThat(contenido.tareas()).singleElement().satisfies(t -> assertThat(t.id()).isEqualTo(tarea.id()));
        assertThat(panel.matriculas()).singleElement().satisfies(m -> {
            assertThat(m.estudianteId()).isEqualTo(estudianteId);
            assertThat(m.notaFinal()).isEqualByComparingTo(new BigDecimal("100.00"));
        });
        assertThat(panel.entregas()).singleElement().satisfies(e -> assertThat(e.nota()).isEqualByComparingTo("1000.00"));
        assertThat(panel.anuncios()).singleElement().satisfies(a -> assertThat(a.cuerpo()).isEqualTo("Instrucciones reales"));
        patch("/api/anuncios/" + anuncio.id() + "/archivar", docenteDuenoToken, AnuncioResponse.class);
        var actualizado = get("/api/cursos/" + curso.id() + "/panel-docente", docenteDuenoToken, PanelCursoResponse.class).getBody();
        assertThat(actualizado.anuncios().getFirst().activo()).isFalse();
    }

    @Test
    void panel_soloDocentePropietarioOAdmin_sinExponerDatosAAjenos() {
        var curso = crearCurso(docenteDuenoId, "Curso privado");
        String path = "/api/cursos/" + curso.id() + "/panel-docente";
        assertThat(get(path, docenteDuenoToken, Object.class).getStatusCode().value()).isEqualTo(200);
        assertThat(get(path, adminToken, Object.class).getStatusCode().value()).isEqualTo(200);
        assertThat(get(path, docenteAjenoToken, Object.class).getStatusCode().value()).isEqualTo(403);
        crearUsuario("Estudiante", "Ajeno", "estudiante.panel@it.invalid", "ESTUDIANTE");
        String estudianteToken = login("estudiante.panel@it.invalid", PASSWORD);
        assertThat(get(path, estudianteToken, Object.class).getStatusCode().value()).isEqualTo(403);
        assertThat(get("/api/cursos/" + UUID.randomUUID() + "/panel-docente", docenteDuenoToken, Object.class).getStatusCode().value()).isEqualTo(404);
    }
}
