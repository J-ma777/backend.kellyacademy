package com.kellyacademy.user.controller;

import com.kellyacademy.user.dto.alumno.AlumnoDtos.*;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.response.*;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.enrollment.dto.response.EntregaResponse;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.communication.entity.Conversacion;
import com.kellyacademy.communication.entity.Mensaje;
import com.kellyacademy.calendar.entity.Tutoria;
import com.kellyacademy.calendar.enums.EstadoTutoria;
import com.kellyacademy.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.assertThat;

class AlumnoDocenteControllerIT extends IntegrationTestBase {
    private static final String BASE="/api/docente/alumnos";
    private Alumno crear(String identificador) {
        var response=post(BASE,docenteDuenoToken,new Crear("Ana María Martínez",identificador,"999159716",PASSWORD),Alumno.class);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return response.getBody();
    }
    private CursoResponse curso(UUID docente,String titulo,int capacidad) {
        var response=post("/api/cursos",adminToken,new CrearCursoRequest(docente,titulo,null,NivelCefr.B2,null,null,null,capacidad),CursoResponse.class);
        assertThat(response.getStatusCode().value()).isEqualTo(201);return response.getBody();
    }
    private Directorio directorio(){return get(BASE,docenteDuenoToken,Directorio.class).getBody();}
    @Test void altaEsInstitucionalConBcrypt12NoDevuelveContrasenaYRechazaDuplicados() {
        var alumno=crear("AnaMartinez");
        assertThat(alumno.correoElectronico()).isEqualTo("anamartinez@kellyacademy.com");
        assertThat(alumno.nombreCompleto()).isEqualTo("Ana María Martínez");
        assertThat(alumno.telefono()).isEqualTo("999159716");
        Usuario guardado=usuarioRepository.findWithRolesById(alumno.id()).orElseThrow();
        assertThat(guardado.getContrasena()).startsWith("$2a$12$");
        assertThat(passwordEncoder.matches(PASSWORD,guardado.getContrasena())).isTrue();
        assertThat(guardado.getRoles()).extracting(r->r.getNombre()).containsExactly("ESTUDIANTE");
        assertThat(post(BASE,docenteDuenoToken,new Crear("Duplicado","ANAMARTINEZ","999159716",PASSWORD),Object.class).getStatusCode().value()).isEqualTo(409);
        assertThat(get(BASE,docenteDuenoToken,String.class).getBody()).doesNotContain(PASSWORD,"contrasena","$2a$");
        assertThat(post(BASE,docenteDuenoToken,new Crear("Correo inválido","ana..martinez","999159716",PASSWORD),Object.class).getStatusCode().value()).isEqualTo(400);
        assertThat(post(BASE,docenteDuenoToken,new Crear("Inválido","correo@externo.com","123","corta"),Object.class).getStatusCode().value()).isEqualTo(400);
    }
    @Test void moduloSoloDocenteYProtegeCuentasAdministrativasOMultiplesRoles() {
        var alumno=crear("permisos");String token=login(alumno.correoElectronico(),PASSWORD);
        for(String ajeno: new String[]{token,adminToken}) {
            assertThat(get(BASE,ajeno,Object.class).getStatusCode().value()).isEqualTo(403);
            assertThat(post(BASE,ajeno,new Crear("Otro","otro","999159716",PASSWORD),Object.class).getStatusCode().value()).isEqualTo(403);
            assertThat(delete(BASE+"/"+alumno.id(),ajeno).getStatusCode().value()).isEqualTo(403);
        }
        assertThat(delete(BASE+"/"+adminId,docenteDuenoToken).getStatusCode().value()).isEqualTo(403);
        assertThat(patch(BASE+"/"+docenteAjenoId+"/estado",docenteDuenoToken,Map.of("estado","SUSPENDIDO"),Object.class).getStatusCode().value()).isEqualTo(403);
        Usuario mixto=usuarioRepository.findWithRolesById(alumno.id()).orElseThrow();
        mixto.setRoles(Set.of(rolRepository.findByNombre("ESTUDIANTE").orElseThrow(),rolRepository.findByNombre("ADMINISTRADOR").orElseThrow()));usuarioRepository.save(mixto);
        assertThat(directorio().alumnos()).isEmpty();
        assertThat(delete(BASE+"/"+alumno.id(),docenteDuenoToken).getStatusCode().value()).isEqualTo(403);
    }
    @Test void estadosBloqueanTokensVigentesYEdicionValidaCorreoDuplicado() {
        var alumno=crear("estados");var otro=crear("otromail");String token=login(alumno.correoElectronico(),PASSWORD);
        assertThat(get("/api/cursos/mis-cursos",token,Object.class).getStatusCode().value()).isEqualTo(200);
        for(String estado:new String[]{"INACTIVO","SUSPENDIDO"}) {
            assertThat(patch(BASE+"/"+alumno.id()+"/estado",docenteDuenoToken,Map.of("estado",estado),Alumno.class).getBody().estado().name()).isEqualTo(estado);
            assertThat(get("/api/cursos/mis-cursos",token,Object.class).getStatusCode().value()).isEqualTo(401);
            assertThat(patch(BASE+"/"+alumno.id()+"/estado",docenteDuenoToken,Map.of("estado","ACTIVO"),Object.class).getStatusCode().value()).isEqualTo(200);
        }
        assertThat(patch(BASE+"/"+alumno.id(),docenteDuenoToken,new Actualizar("Otro nombre",otro.correoElectronico(),"987654321"),Object.class).getStatusCode().value()).isEqualTo(409);
        var actual=patch(BASE+"/"+alumno.id(),docenteDuenoToken,new Actualizar("Ana Actualizada","correo.nuevo@kellyacademy.com","987654321"),Alumno.class).getBody();
        assertThat(actual.telefono()).isEqualTo("987654321");
        assertThat(get("/api/cursos/mis-cursos",token,Object.class).getStatusCode().value()).isEqualTo(401);
        assertThat(login(actual.correoElectronico(),PASSWORD)).isNotBlank();
    }
    @Test void matriculacionMulticursoConcurrenteRetiraYReactivaConMismoHistorial() {
        var alumno=crear("multicurso");var uno=curso(docenteDuenoId,"Curso propio",10);var dos=curso(docenteAjenoId,"Curso de otro docente",10);
        var primera=CompletableFuture.supplyAsync(()->post(BASE+"/"+alumno.id()+"/cursos",docenteDuenoToken,new Inscribir(uno.id()),Object.class));
        var segunda=CompletableFuture.supplyAsync(()->post(BASE+"/"+alumno.id()+"/cursos",docenteDuenoToken,new Inscribir(dos.id()),Object.class));
        assertThat(primera.join().getStatusCode().value()).isEqualTo(204);assertThat(segunda.join().getStatusCode().value()).isEqualTo(204);
        var panel=directorio();assertThat(panel.cursos()).extracting(Curso::id).contains(uno.id(),dos.id());
        assertThat(panel.alumnos().getFirst().matriculas()).hasSize(2);
        var matricula=matriculaRepository.findByCursoIdAndEstudianteId(uno.id(),alumno.id()).orElseThrow();
        matricula.setEstado(EstadoMatricula.RIESGO);matriculaRepository.save(matricula);
        assertThat(directorio().alumnos().getFirst().enRiesgo()).isTrue();
        assertThat(delete(BASE+"/"+alumno.id()+"/cursos/"+uno.id(),docenteDuenoToken).getStatusCode().value()).isEqualTo(204);
        assertThat(matriculaRepository.findById(matricula.getId()).orElseThrow().getEstado()).isEqualTo(EstadoMatricula.ABANDONADA);
        assertThat(post(BASE+"/"+alumno.id()+"/cursos",docenteDuenoToken,new Inscribir(uno.id()),Object.class).getStatusCode().value()).isEqualTo(204);
        assertThat(matriculaRepository.findByCursoIdAndEstudianteId(uno.id(),alumno.id()).orElseThrow().getId()).isEqualTo(matricula.getId());
    }
    @Test void cuposNoSeSobreasignanConAltasConcurrentesYCursoInactivoSeRechaza() {
        var alumno=crear("cupo1");var otro=crear("cupo2");var unico=curso(docenteDuenoId,"Único cupo",1);
        var uno=CompletableFuture.supplyAsync(()->post(BASE+"/"+alumno.id()+"/cursos",docenteDuenoToken,new Inscribir(unico.id()),Object.class));
        var dos=CompletableFuture.supplyAsync(()->post(BASE+"/"+otro.id()+"/cursos",docenteDuenoToken,new Inscribir(unico.id()),Object.class));
        assertThat(java.util.List.of(uno.join().getStatusCode().value(),dos.join().getStatusCode().value())).containsExactlyInAnyOrder(204,400);
        assertThat(matriculaRepository.countByCursoIdAndEstado(unico.id(),EstadoMatricula.ACTIVA)).isEqualTo(1);
        desactivarCurso(unico.id());
        UUID noInscrito=matriculaRepository.existsByCursoIdAndEstudianteId(unico.id(),alumno.id())?otro.id():alumno.id();
        assertThat(post(BASE+"/"+noInscrito+"/cursos",docenteDuenoToken,new Inscribir(unico.id()),Object.class).getStatusCode().value()).isEqualTo(400);
    }
    @Test void eliminacionDefinitivaCascadaEntregasNotasMatriculasMensajesYProgreso() {
        var alumno=crear("eliminar");String token=login(alumno.correoElectronico(),PASSWORD);var curso=curso(docenteDuenoId,"Historial",10);
        post(BASE+"/"+alumno.id()+"/cursos",docenteDuenoToken,new Inscribir(curso.id()),Object.class);
        var unidad=post("/api/unidades",docenteDuenoToken,Map.of("cursoId",curso.id(),"numero",1,"titulo","Unidad"),UnidadResponse.class).getBody();
        var semana=post("/api/semanas",docenteDuenoToken,Map.of("unidadId",unidad.id(),"numero",1,"titulo","Semana"),SemanaResponse.class).getBody();
        var tarea=post("/api/tareas",docenteDuenoToken,Map.of("semanaId",semana.id(),"titulo","Actividad","puntajeMaximo",100),TareaResponse.class).getBody();
        var material=post("/api/materiales",docenteDuenoToken,Map.of("semanaId",semana.id(),"titulo","Material","tipo","PDF","urlExterno","https://example.org/real.pdf"),MaterialResponse.class).getBody();
        var entrega=post("/api/entregas/enviar",token,Map.of("tareaId",tarea.id(),"textoRespuesta","Trabajo evaluado"),EntregaResponse.class).getBody();
        patch("/api/entregas/"+entrega.id()+"/calificar",docenteDuenoToken,Map.of("nota",100),Object.class);
        post("/api/cursos/"+curso.id()+"/contenido/MATERIAL/"+material.id()+"/completar",token,Map.of(),Object.class);
        Usuario estudiante=usuarioRepository.getReferenceById(alumno.id()),docente=usuarioRepository.getReferenceById(docenteDuenoId);
        Conversacion conversacion=new Conversacion();conversacion.setParticipante1(estudiante);conversacion.setParticipante2(docente);conversacion=conversacionRepository.save(conversacion);
        Mensaje mensaje=new Mensaje();mensaje.setConversacion(conversacion);mensaje.setRemitente(docente);mensaje.setCuerpo("Mensaje relacionado");mensaje.setEnviadoAt(LocalDateTime.now());mensajeRepository.save(mensaje);
        Tutoria tutoria=new Tutoria();tutoria.setEstudiante(estudiante);tutoria.setDocente(docente);tutoria.setEstado(EstadoTutoria.PENDIENTE);tutoria.setFecha(LocalDateTime.now().plusDays(1));tutoria.setDuracionMinutos(30);tutoriaRepository.save(tutoria);
        assertThat(delete(BASE+"/"+alumno.id(),docenteDuenoToken).getStatusCode().value()).isEqualTo(204);
        assertThat(usuarioRepository.existsById(alumno.id())).isFalse();assertThat(matriculaRepository.findByEstudianteId(alumno.id())).isEmpty();assertThat(entregaRepository.findByEstudianteId(alumno.id())).isEmpty();
        assertThat(conversacionRepository.count()).isZero();assertThat(mensajeRepository.count()).isZero();assertThat(tutoriaRepository.count()).isZero();
        assertThat(cursoRepository.existsById(curso.id())).isTrue();assertThat(usuarioRepository.existsById(docenteDuenoId)).isTrue();
        assertThat(get("/api/cursos/mis-cursos",token,Object.class).getStatusCode().value()).isEqualTo(401);
    }
}
