package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.response.*;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.enrollment.dto.response.EntregaResponse;
import com.kellyacademy.communication.dto.response.AnuncioResponse;
import com.kellyacademy.support.IntegrationTestBase;
import com.kellyacademy.shared.config.AppTime;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class AulaCursoControllerIT extends IntegrationTestBase {
    private record Aula(UUID curso, UUID semana, UUID alumno, String token, UUID companero, String tokenCompanero) {}
    private Aula preparar() {
        var curso = post("/api/cursos", adminToken, new CrearCursoRequest(docenteDuenoId, "Curso aula", "Descripción", NivelCefr.B1,
                null, null, null, 20), CursoResponse.class).getBody();
        var unidad = post("/api/unidades", docenteDuenoToken, Map.of("cursoId", curso.id(), "numero", 1, "titulo", "Módulo"), UnidadResponse.class).getBody();
        var semana = post("/api/semanas", docenteDuenoToken, Map.of("unidadId", unidad.id(), "numero", 1, "titulo", "Semana"), SemanaResponse.class).getBody();
        UUID alumno = crearUsuario("Ana", "Aula", "ana.aula@it.invalid", "ESTUDIANTE");
        UUID companero = crearUsuario("Luis", "Aula", "luis.aula@it.invalid", "ESTUDIANTE");
        assertThat(post("/api/matriculas", adminToken, Map.of("cursoId", curso.id(), "estudianteId", alumno), Object.class).getStatusCode().value()).isEqualTo(201);
        assertThat(post("/api/matriculas", adminToken, Map.of("cursoId", curso.id(), "estudianteId", companero), Object.class).getStatusCode().value()).isEqualTo(201);
        return new Aula(curso.id(), semana.id(), alumno, login("ana.aula@it.invalid", PASSWORD), companero, login("luis.aula@it.invalid", PASSWORD));
    }
    private String ruta(Aula a) { return "/api/cursos/" + a.curso(); }
    private PanelCursoResponse panel(Aula a, String token) { return get(ruta(a)+"/aula", token, PanelCursoResponse.class).getBody(); }
    private void crearMaterial(Aula a, boolean publicado, boolean visible, String titulo) {
        assertThat(post(ruta(a)+"/contenido/MATERIAL", docenteDuenoToken,
                Map.of("semanaId", a.semana(), "titulo", titulo, "tipo", "PDF", "urlExterno", "https://example.org/documento.pdf", "publicado", publicado, "visible", visible), Object.class).getStatusCode().value()).isEqualTo(201);
    }
    @Test
    void visibilidadSeValidaEnServidorYElProgresoEsPersistenteEIdempotente() {
        Aula a = preparar();
        var matricula = panel(a, docenteDuenoToken).matriculas().stream().filter(m -> m.estudianteId().equals(a.alumno())).findFirst().orElseThrow();
        assertThat(patch("/api/matriculas/"+matricula.id()+"/estado",adminToken,Map.of("estado","RIESGO"),Object.class).getStatusCode().value()).isEqualTo(200);
        var misCursos=get("/api/cursos/mis-cursos",a.token(),String.class);
        assertThat(misCursos.getStatusCode().value()).isEqualTo(200);
        assertThat(misCursos.getBody()).contains(a.curso().toString());
        crearMaterial(a, false, true, "Borrador");
        crearMaterial(a, true, false, "Oculto");
        crearMaterial(a, true, true, "Lectura pública");
        assertThat(post(ruta(a)+"/contenido/CLASS", docenteDuenoToken, Map.of("semanaId",a.semana(),"titulo","Clase secreta", "fechaHora",LocalDateTime.now(AppTime.ZONA_NEGOCIO).toString(),"duracionMinutos",30,"publicado",false,"visible",true),Object.class).getStatusCode().value()).isEqualTo(201);
        var dashboard=get("/api/dashboard/resumen",a.token(),String.class);
        assertThat(dashboard.getStatusCode().value()).isEqualTo(200);
        assertThat(dashboard.getBody()).doesNotContain("Clase secreta");
        var docente = panel(a, docenteDuenoToken);
        assertThat(docente.puedeGestionar()).isTrue();
        var materiales = docente.unidades().getFirst().semanas().getFirst().materiales();
        assertThat(materiales).hasSize(3);
        var publico = materiales.stream().filter(m -> m.titulo().equals("Lectura pública")).findFirst().orElseThrow();
        var borrador = materiales.stream().filter(m -> m.titulo().equals("Borrador")).findFirst().orElseThrow();
        var alumno = panel(a, a.token());
        assertThat(alumno.puedeGestionar()).isFalse();
        assertThat(alumno.unidades().getFirst().semanas().getFirst().materiales()).extracting(MaterialResponse::titulo).containsExactly("Lectura pública");
        assertThat(get("/api/materiales/"+borrador.id(), a.token(), Object.class).getStatusCode().value()).isEqualTo(403);
        assertThat(get("/api/materiales?semanaId="+a.semana(), a.token(), String.class).getBody()).doesNotContain("Borrador", "Oculto");
        for(int i=0;i<2;i++) assertThat(post(ruta(a)+"/contenido/MATERIAL/"+publico.id()+"/completar", a.token(), Map.of(), Object.class).getStatusCode().value()).isEqualTo(204);
        var actualizado = panel(a, docenteDuenoToken);
        assertThat(actualizado.avances().stream().filter(p -> p.estudianteId().equals(a.alumno())).findFirst().orElseThrow().progreso()).isEqualByComparingTo("100");
        assertThat(actualizado.estadisticas().progresoPromedio()).isEqualByComparingTo("50");
        assertThat(panel(a, a.token()).contenidosCompletados()).containsExactly(publico.id());
        assertThat(panel(a, a.tokenCompanero()).avances().stream().filter(p -> p.estudianteId().equals(a.alumno())).findFirst().orElseThrow().progreso()).isNull();
        assertThat(patch(ruta(a)+"/contenido/MATERIAL/"+publico.id()+"/publicacion", a.token(), Map.of("publicado", true,"visible",false), Object.class).getStatusCode().value()).isEqualTo(403);
        assertThat(get(ruta(a)+"/aula", docenteAjenoToken, Object.class).getStatusCode().value()).isEqualTo(403);
        assertThat(patch(ruta(a)+"/contenido/MATERIAL/"+borrador.id()+"/publicacion", docenteDuenoToken, Map.of("publicado",true,"visible",true),Object.class).getStatusCode().value()).isEqualTo(204);
        assertThat(panel(a,a.token()).unidades().getFirst().semanas().getFirst().materiales()).hasSize(2);
    }
    @Test
    void alumnoEnviaTextoTardioDocenteCalificaYRetiroConservaElHistorial() {
        Aula a = preparar();
        assertThat(post(ruta(a)+"/contenido/TASK", docenteDuenoToken, Map.of("semanaId",a.semana(),"titulo","Evaluación", "descripcion","Responde con tus conclusiones", "puntajeMaximo",1000,
                "fechaLimite",LocalDateTime.now(AppTime.ZONA_NEGOCIO).minusDays(1).toString(),"tipoActividad","EVALUACION","publicado",true,"visible",true),Object.class).getStatusCode().value()).isEqualTo(201);
        var tarea = panel(a,a.token()).unidades().getFirst().semanas().getFirst().tareas().getFirst();
        assertThat(tarea.tipoActividad().name()).isEqualTo("EVALUACION");
        assertThat(post("/api/entregas/enviar",a.token(),Map.of("tareaId",tarea.id()),Object.class).getStatusCode().is4xxClientError()).isTrue();
        var response = post("/api/entregas/enviar",a.token(),Map.of("tareaId",tarea.id(),"textoRespuesta","Mi respuesta real"),EntregaResponse.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var entrega = response.getBody();
        assertThat(entrega.estudianteId()).isEqualTo(a.alumno());
        assertThat(entrega.textoRespuesta()).isEqualTo("Mi respuesta real");
        assertThat(entrega.tardia()).isTrue();
        assertThat(post("/api/entregas/enviar",a.token(),Map.of("tareaId",tarea.id(),"textoRespuesta","Respuesta corregida"),EntregaResponse.class).getBody().id()).isEqualTo(entrega.id());
        assertThat(panel(a,a.tokenCompanero()).entregas()).isEmpty();
        assertThat(patch("/api/entregas/"+entrega.id()+"/calificar",a.tokenCompanero(),Map.of("nota",1000),Object.class).getStatusCode().value()).isEqualTo(403);
        var calificada=patch("/api/entregas/"+entrega.id()+"/calificar",docenteDuenoToken,Map.of("nota",1000,"retroalimentacion","Muy completo"),EntregaResponse.class).getBody();
        assertThat(calificada.nota()).isEqualByComparingTo("1000");
        assertThat(calificada.tardia()).isTrue();
        assertThat(calificada.calificadoAt()).isNotNull();
        assertThat(put(ruta(a)+"/contenido/TASK/"+tarea.id(),docenteDuenoToken,
                Map.of("semanaId",a.semana(),"titulo","Evaluación","puntajeMaximo",100,"publicado",true,"visible",true),Object.class).getStatusCode().is4xxClientError()).isTrue();
        assertThat(panel(a,a.token()).matriculas().stream().filter(m->m.estudianteId().equals(a.alumno())).findFirst().orElseThrow().notaFinal()).isEqualByComparingTo("100");
        assertThat(panel(a,a.tokenCompanero()).matriculas().stream().filter(m->m.estudianteId().equals(a.alumno())).findFirst().orElseThrow().notaFinal()).isNull();
        assertThat(post("/api/entregas/enviar",a.token(),Map.of("tareaId",tarea.id(),"textoRespuesta","Cambio tras nota"),Object.class).getStatusCode().is4xxClientError()).isTrue();
        assertThat(delete(ruta(a)+"/alumnos/"+a.alumno(),a.tokenCompanero()).getStatusCode().value()).isEqualTo(403);
        assertThat(delete(ruta(a)+"/alumnos/"+a.alumno(),docenteDuenoToken).getStatusCode().value()).isEqualTo(204);
        assertThat(get(ruta(a)+"/aula",a.token(),Object.class).getStatusCode().value()).isEqualTo(403);
        assertThat(post("/api/entregas/enviar",a.token(),Map.of("tareaId",tarea.id(),"textoRespuesta","Tras retiro"),Object.class).getStatusCode().value()).isEqualTo(403);
        assertThat(panel(a,docenteDuenoToken).entregas()).hasSize(1);
    }
    @Test
    void anunciosNoExponenBorradoresSeOrdenanFijadosYSanitizanHtml() {
        Aula a=preparar();
        var borrador=post("/api/anuncios",docenteDuenoToken,Map.of("cursoId",a.curso(),"titulo","Borrador privado","cuerpo","<p><strong>Aviso</strong><script>alert(1)</script><a href='javascript:alert(1)'>enlace</a></p>","publicado",false,"fijado",false),AnuncioResponse.class).getBody();
        assertThat(borrador.cuerpo()).contains("<strong>Aviso</strong>").doesNotContain("script","javascript");
        assertThat(notificacionRepository.count()).isZero();
        assertThat(panel(a,a.token()).anuncios()).isEmpty();
        assertThat(get("/api/anuncios/"+borrador.id(),a.token(),Object.class).getStatusCode().value()).isEqualTo(403);
        assertThat(get("/api/anuncios?cursoId="+a.curso(),a.token(),String.class).getBody()).doesNotContain("Borrador privado");
        post("/api/anuncios",docenteDuenoToken,Map.of("cursoId",a.curso(),"titulo","Fijado","cuerpo","Fijado real","publicado",true,"fijado",true),AnuncioResponse.class);
        var publicado=put("/api/anuncios/"+borrador.id(),docenteDuenoToken,Map.of("titulo","Publicado reciente","cuerpo","<p>Ya disponible</p>","publicado",true,"fijado",false),AnuncioResponse.class).getBody();
        assertThat(publicado.publicadoAt()).isNotNull();
        assertThat(panel(a,a.token()).anuncios()).extracting(AnuncioResponse::titulo).containsExactly("Fijado","Publicado reciente");
        assertThat(notificacionRepository.count()).isEqualTo(4);
    }
}
