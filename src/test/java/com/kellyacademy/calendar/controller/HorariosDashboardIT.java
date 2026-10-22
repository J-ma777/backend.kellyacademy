package com.kellyacademy.calendar.controller;

import com.kellyacademy.support.IntegrationTestBase;
import com.kellyacademy.course.dto.request.*;
import com.kellyacademy.course.dto.response.*;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.calendar.service.AgendaService;
import com.kellyacademy.course.service.HorarioCursoService;
import com.kellyacademy.dashboard.dto.DashboardResumenResponse;
import com.kellyacademy.shared.config.AppTime;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class HorariosDashboardIT extends IntegrationTestBase {
    private CursoResponse curso(UUID docente,String titulo,LocalDate inicio,LocalDate fin,List<HorarioCursoRequest> horarios) {
        var respuesta=post("/api/cursos",adminToken,new CrearCursoRequest(docente,titulo,null,NivelCefr.B1,null,inicio,fin,20,horarios),CursoResponse.class);
        assertThat(respuesta.getStatusCode().value()).isEqualTo(201);return respuesta.getBody();
    }
    private HorarioCursoRequest bloque(DayOfWeek dia,int inicio,int fin){return new HorarioCursoRequest(dia,LocalTime.of(inicio,0),LocalTime.of(fin,0));}
    private String agenda(LocalDate desde,LocalDate hasta){return "/api/calendario?desde="+desde+"&hasta="+hasta;}
    private String alumno(UUID curso,String correo) {
        UUID id=crearUsuario("Alumna","Horario",correo,"ESTUDIANTE");
        assertThat(post("/api/matriculas",adminToken,Map.of("cursoId",curso,"estudianteId",id),Object.class).getStatusCode().value()).isEqualTo(201);
        return login(correo,PASSWORD);
    }
    @Test void alumnoVeUnoDeDiezYDocenteVeTotalSinMezclarSesiones() {
        LocalDate hoy=LocalDate.now(AppTime.ZONA_NEGOCIO);
        var propio=curso(docenteDuenoId,"Su único curso",hoy,hoy.plusDays(20),List.of(bloque(hoy.getDayOfWeek(),8,9)));
        for(int i=0;i<9;i++)curso(i%2==0?docenteAjenoId:docenteDuenoId,"Otro curso "+i,null,null,null);
        String token=alumno(propio.id(),"uno.diez@it.invalid");
        var docente=get("/api/dashboard/resumen",docenteDuenoToken,DashboardResumenResponse.class).getBody();
        assertThat(docente.metricas().cursosInscritos()).isEqualTo(10);
        var estudiante=get("/api/dashboard/resumen",token,DashboardResumenResponse.class).getBody();
        assertThat(estudiante.metricas().cursosInscritos()).isEqualTo(1);
        assertThat(estudiante.clasesHoy()).singleElement().satisfies(c->assertThat(c.cursoTitulo()).isEqualTo("Su único curso"));
        UUID ajeno=crearUsuario("Sin","Matrícula","sin.cursos@it.invalid","ESTUDIANTE");String sin=login("sin.cursos@it.invalid",PASSWORD);
        assertThat(get("/api/dashboard/resumen",sin,DashboardResumenResponse.class).getBody().metricas().cursosInscritos()).isZero();
        assertThat(get(agenda(hoy,hoy.plusDays(6)),sin,AgendaService.Item[].class).getBody()).isEmpty();
        var matricula=matriculaRepository.findByEstudianteId(usuarioRepository.findByCorreoElectronico("uno.diez@it.invalid").orElseThrow().getId()).getFirst();
        assertThat(patch("/api/matriculas/"+matricula.getId()+"/estado",adminToken,Map.of("estado","COMPLETADA"),Object.class).getStatusCode().value()).isEqualTo(200);
        assertThat(get("/api/dashboard/resumen",token,DashboardResumenResponse.class).getBody().metricas().cursosInscritos()).isEqualTo(1);
        assertThat(delete("/api/docente/alumnos/"+matricula.getEstudiante().getId()+"/cursos/"+propio.id(),docenteDuenoToken).getStatusCode().value()).isEqualTo(204);
        assertThat(get("/api/dashboard/resumen",token,DashboardResumenResponse.class).getBody().metricas().cursosInscritos()).isZero();
    }
    @Test void horarioPersisteSeRepiteSoloEnFechasDelCursoYRespetaZonaLima() {
        LocalDate lunes=LocalDate.now(AppTime.ZONA_NEGOCIO).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        var curso=curso(docenteDuenoId,"Curso semanal",lunes,lunes.plusDays(9),List.of(bloque(DayOfWeek.MONDAY,19,20),bloque(DayOfWeek.WEDNESDAY,19,21)));
        assertThat(curso.horario()).isEqualTo("Lun 19:00–20:00 · Mié 19:00–21:00");
        var plan=get("/api/cursos/"+curso.id()+"/horarios",docenteDuenoToken,HorarioCursoService.Plan.class).getBody();assertThat(plan.horarios()).hasSize(2);
        String token=alumno(curso.id(),"agenda@it.invalid");
        var items=get(agenda(lunes.minusDays(1),lunes.plusDays(20)),token,AgendaService.Item[].class).getBody();
        assertThat(items).hasSize(4);assertThat(items).allSatisfy(i->{assertThat(i.cursoId()).isEqualTo(curso.id());var local=i.inicio().atZoneSameInstant(AppTime.ZONA_NEGOCIO);assertThat(local.getOffset()).isEqualTo(ZoneOffset.ofHours(-5));assertThat(local.toLocalTime()).isEqualTo(LocalTime.of(19,0));assertThat(local.toLocalDate()).isBetween(lunes,lunes.plusDays(9));});
        assertThat(get(agenda(lunes,lunes.plusDays(90)),token,Object.class).getStatusCode().value()).isEqualTo(400);
        assertThat(get(agenda(lunes,lunes.plusDays(5)),docenteAjenoToken,AgendaService.Item[].class).getBody()).isEmpty();
    }
    @Test void rechazaFranjasInvalidasCrucesYPermiteHorariosAdyacentesOEnOtroPeriodo() {
        LocalDate lunes=LocalDate.now(AppTime.ZONA_NEGOCIO).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        var primero=curso(docenteDuenoId,"Primero",lunes,lunes.plusDays(14),List.of(bloque(DayOfWeek.MONDAY,18,20)));
        for(var bloques:List.of(List.of(bloque(DayOfWeek.MONDAY,20,19)),List.of(bloque(DayOfWeek.TUESDAY,18,20),bloque(DayOfWeek.TUESDAY,19,21)),List.of(bloque(DayOfWeek.MONDAY,19,21)))) {
            assertThat(post("/api/cursos",adminToken,new CrearCursoRequest(docenteDuenoId,"Inválido",null,NivelCefr.B1,null,lunes,lunes.plusDays(14),20,bloques),Object.class).getStatusCode().value()).isEqualTo(400);
        }
        assertThat(cursoRepository.count()).isEqualTo(1);
        curso(docenteDuenoId,"Adyacente",lunes,lunes.plusDays(14),List.of(bloque(DayOfWeek.MONDAY,20,21)));
        curso(docenteDuenoId,"Otro periodo",lunes.plusDays(21),lunes.plusDays(40),List.of(bloque(DayOfWeek.MONDAY,18,20)));
        assertThat(post("/api/cursos",adminToken,new CrearCursoRequest(docenteDuenoId,"Sin inicio",null,NivelCefr.B1,null,null,null,20,List.of(bloque(DayOfWeek.FRIDAY,10,11))),Object.class).getStatusCode().value()).isEqualTo(400);
    }
    @Test void editarHorarioReemplazaSesionesYSoloPropietarioOAdminPuedeModificarlo() {
        LocalDate lunes=LocalDate.now(AppTime.ZONA_NEGOCIO).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        var curso=curso(docenteDuenoId,"Editar",lunes,lunes.plusDays(6),List.of(bloque(DayOfWeek.MONDAY,8,9)));
        String token=alumno(curso.id(),"edicion@it.invalid");
        var cambio=new GuardarHorarioCursoRequest(lunes,lunes.plusDays(6),List.of(bloque(DayOfWeek.FRIDAY,10,11)));
        String path="/api/cursos/"+curso.id()+"/horarios";
        assertThat(put(path,token,cambio,Object.class).getStatusCode().value()).isEqualTo(403);
        assertThat(put(path,docenteAjenoToken,cambio,Object.class).getStatusCode().value()).isEqualTo(403);
        assertThat(put(path,docenteDuenoToken,cambio,Object.class).getStatusCode().value()).isEqualTo(204);
        var items=get(agenda(lunes,lunes.plusDays(6)),token,AgendaService.Item[].class).getBody();assertThat(items).singleElement().satisfies(i->assertThat(i.inicio().getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY));
        assertThat(get(path,token,HorarioCursoService.Plan.class).getBody().horarios()).hasSize(1);
    }
    @Test void claseConcretaReemplazaOcurrenciaSemanalYOcultosNoSeFiltranAlAlumno() {
        LocalDate lunes=LocalDate.now(AppTime.ZONA_NEGOCIO).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        var curso=curso(docenteDuenoId,"Clase real",lunes,lunes.plusDays(6),List.of(bloque(DayOfWeek.MONDAY,19,20)));
        String token=alumno(curso.id(),"clase.real@it.invalid");
        var unidad=post("/api/unidades",docenteDuenoToken,Map.of("cursoId",curso.id(),"numero",1,"titulo","Unidad"),UnidadResponse.class).getBody();
        var semana=post("/api/semanas",docenteDuenoToken,Map.of("unidadId",unidad.id(),"numero",1,"titulo","Semana"),SemanaResponse.class).getBody();
        var clase=post("/api/clases",docenteDuenoToken,Map.of("semanaId",semana.id(),"titulo","Clase con enlace real","fechaHora",lunes.atTime(19,0).toString(),"duracionMinutos",60,"urlVivo","https://example.org/sala"),ClaseResponse.class).getBody();
        assertThat(post("/api/cursos/"+curso.id()+"/contenido/CLASS",docenteDuenoToken,Map.of("semanaId",semana.id(),"titulo","Clase oculta","fechaHora",lunes.atTime(12,0).toString(),"duracionMinutos",60,"publicado",false,"visible",true),Object.class).getStatusCode().value()).isEqualTo(201);
        var items=get(agenda(lunes,lunes.plusDays(6)),token,AgendaService.Item[].class).getBody();
        assertThat(items).singleElement().satisfies(i->{assertThat(i.id()).isEqualTo(clase.id());assertThat(i.tipo()).isEqualTo("CLASE");assertThat(i.url()).isEqualTo("https://example.org/sala");});
    }
}
