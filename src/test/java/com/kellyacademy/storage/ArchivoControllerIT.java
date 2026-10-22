package com.kellyacademy.storage;

import com.kellyacademy.support.IntegrationTestBase;
import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.enums.NivelCefr;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArchivoControllerIT extends IntegrationTestBase {
    @MockitoBean CloudinaryGateway cloudinary;
    @MockitoBean R2Gateway r2;
    @MockitoBean OfficePreviewGateway office;
    @Autowired ArchivoRepository archivos;
    private UUID curso(){
        return post("/api/cursos",adminToken,new CrearCursoRequest(docenteDuenoId,"Archivos",null,NivelCefr.A1,null,null,null,20),CursoResponse.class).getBody().id();
    }
    private ResponseEntity<Map> subir(UUID curso,String token,String destino,String name,byte[] data){
        var body=new LinkedMultiValueMap<String,Object>();
        body.add("archivo",new ByteArrayResource(data){@Override public String getFilename(){return name;}});
        var headers=headersConToken(token);headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return rest.exchange("/api/cursos/"+curso+"/archivos?destino="+destino,HttpMethod.POST,new HttpEntity<>(body,headers),Map.class);
    }
    private void proveedor(UUID curso){
        when(r2.upload(any(),anyString(),anyString(),eq(curso))).thenAnswer(i->"cursos/"+curso+"/"+UUID.randomUUID()+".pdf");
        when(r2.downloadUrl(anyString())).thenReturn("https://private.example.test/document.pdf?signature=temporal");
        when(cloudinary.upload(any(),anyString(),anyString(),eq(curso))).thenAnswer(i->{String id="image/"+UUID.randomUUID();return new CloudinaryGateway.Uploaded(id,"image","https://res.cloudinary.com/test/image/authenticated/s--signature--/"+id+".png");});
    }
    @Test void docenteSubePdfYSePersistenMetadatosSinBinario(){
        UUID curso=curso();proveedor(curso);
        var response=subir(curso,docenteDuenoToken,"MATERIAL","guia.pdf","%PDF-1.7\nprueba".getBytes(StandardCharsets.UTF_8));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        var persisted=archivos.findById(UUID.fromString((String)response.getBody().get("id"))).orElseThrow();
        assertThat(persisted.getCursoId()).isEqualTo(curso);assertThat(persisted.getUsuarioId()).isEqualTo(docenteDuenoId);
        assertThat(persisted.getProveedor()).isEqualTo("R2");
        assertThat(persisted.getUrl()).doesNotContain("signature=").contains("/api/cursos/");
        assertThat(persisted.getNombre()).isEqualTo("guia.pdf");assertThat(persisted.getMime()).isEqualTo("application/pdf");
        assertThat(persisted.getUrl()).isEqualTo(response.getBody().get("url"));assertThat(persisted.getBytes()).isEqualTo(15);
    }
    @Test void controlaPropiedadYMatriculaAntesDeContactarProveedor(){
        UUID curso=curso();
        assertThat(subir(curso,docenteAjenoToken,"MATERIAL","guia.pdf","%PDF-1.7".getBytes()).getStatusCode().value()).isEqualTo(403);
        UUID alumno=crearUsuario("Alumno","Archivo","archivo@it.invalid","ESTUDIANTE");String token=login("archivo@it.invalid",PASSWORD);
        assertThat(subir(curso,token,"ENTREGA","guia.pdf","%PDF-1.7".getBytes()).getStatusCode().value()).isEqualTo(403);
        post("/api/matriculas",adminToken,Map.of("cursoId",curso,"estudianteId",alumno),Object.class);
        assertThat(subir(curso,token,"MATERIAL","guia.pdf","%PDF-1.7".getBytes()).getStatusCode().value()).isEqualTo(403);
        verifyNoInteractions(cloudinary,r2);
        proveedor(curso);
        assertThat(subir(curso,token,"ENTREGA","respuesta.pdf","%PDF-1.7".getBytes()).getStatusCode().value()).isEqualTo(201);
        assertThat(archivos.findAll()).singleElement().satisfies(a->assertThat(a.getUsuarioId()).isEqualTo(alumno));
    }
    @Test void rechazaContenidoFalsoYErroresNoDejanMetadatos(){
        UUID curso=curso();
        assertThat(subir(curso,docenteDuenoToken,"MATERIAL","falso.pdf","no es un pdf".getBytes()).getStatusCode().value()).isEqualTo(400);
        assertThat(subir(curso,docenteDuenoToken,"MATERIAL","archivo.exe",new byte[]{1,2,3}).getStatusCode().value()).isEqualTo(400);
        assertThat(subir(curso,docenteDuenoToken,"MATERIAL","vacio.txt",new byte[0]).getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(cloudinary,r2);
        when(r2.upload(any(),anyString(),anyString(),eq(curso))).thenThrow(new com.kellyacademy.shared.exception.BusinessException("SUBIDA_FALLIDA","Proveedor no disponible"));
        assertThat(subir(curso,docenteDuenoToken,"MATERIAL","real.pdf","%PDF-1.7".getBytes()).getStatusCode().value()).isEqualTo(400);
        assertThat(archivos.count()).isZero();
    }
    private UUID semana(UUID curso){
        var unidad=post("/api/unidades",docenteDuenoToken,Map.of("cursoId",curso,"numero",1,"titulo","Unidad"),Map.class).getBody();
        var semana=post("/api/semanas",docenteDuenoToken,Map.of("unidadId",unidad.get("id"),"numero",1,"titulo","Semana"),Map.class).getBody();
        return UUID.fromString((String)semana.get("id"));
    }
    private String matricular(UUID curso,String email){
        UUID estudiante=crearUsuario("Alumno","Adjunto",email,"ESTUDIANTE");
        assertThat(post("/api/matriculas",adminToken,Map.of("cursoId",curso,"estudianteId",estudiante),Object.class).getStatusCode().value()).isEqualTo(201);
        return login(email,PASSWORD);
    }
    private byte[] png(){return new byte[]{(byte)137,80,78,71,13,10,26,10};}
    @Test void instruccionesPrivadasSoloSeAbrenAlPublicarLaActividadYSeGeneranEnlacesNuevos(){
        UUID curso=curso();proveedor(curso);UUID semana=semana(curso);
        String alumno=matricular(curso,"guia.r2@it.invalid");
        var archivo=subir(curso,docenteDuenoToken,"ACTIVIDAD","guia.pdf","%PDF-1.7".getBytes()).getBody();
        String url=(String)archivo.get("url");String ruta="/api/cursos/"+curso+"/archivos/"+archivo.get("id")+"/acceso";
        assertThat(get(ruta,docenteDuenoToken,Map.class).getStatusCode().value()).isEqualTo(200);
        assertThat(get(ruta,alumno,Map.class).getStatusCode().value()).isEqualTo(403);
        var datos=new HashMap<String,Object>();datos.put("semanaId",semana);datos.put("titulo","Actividad con guía");datos.put("urlExterno",url);datos.put("puntajeMaximo",20);datos.put("publicado",false);datos.put("visible",true);
        assertThat(post("/api/cursos/"+curso+"/contenido/TASK",docenteDuenoToken,datos,Object.class).getStatusCode().value()).isEqualTo(201);
        UUID tarea=tareaRepository.findBySemanaIdOrderByFechaLimiteAsc(semana).getFirst().getId();
        assertThat(get(ruta,alumno,Map.class).getStatusCode().value()).isEqualTo(403);
        assertThat(patch("/api/cursos/"+curso+"/contenido/TASK/"+tarea+"/publicacion",docenteDuenoToken,Map.of("publicado",true,"visible",true),Object.class).getStatusCode().value()).isEqualTo(204);
        var enlace=get(ruta,alumno,Map.class);assertThat(enlace.getStatusCode().value()).isEqualTo(200);
        assertThat(enlace.getBody().get("expiraEnSegundos")).isEqualTo(300);
        assertThat(enlace.getHeaders().getCacheControl()).contains("no-store");
        assertThat(get(ruta,docenteAjenoToken,Map.class).getStatusCode().value()).isEqualTo(403);
    }
    @Test void entregaPdfOImagenEsDelAlumnoYNoSePuedeCopiarLaDeOtro(){
        UUID curso=curso();proveedor(curso);UUID semana=semana(curso);
        var task=post("/api/cursos/"+curso+"/contenido/TASK",docenteDuenoToken,Map.of("semanaId",semana,"titulo","Responde","puntajeMaximo",20,"publicado",true,"visible",true),Object.class);
        assertThat(task.getStatusCode().value()).isEqualTo(201);UUID tarea=tareaRepository.findBySemanaIdOrderByFechaLimiteAsc(semana).getFirst().getId();
        String a=matricular(curso,"respuesta.a@it.invalid"),b=matricular(curso,"respuesta.b@it.invalid");
        var pdf=subir(curso,a,"ENTREGA","respuesta.pdf","%PDF-1.7".getBytes()).getBody();
        String ruta="/api/cursos/"+curso+"/archivos/"+pdf.get("id")+"/acceso";
        assertThat(post("/api/entregas/enviar",a,Map.of("tareaId",tarea,"urlArchivo",pdf.get("url")),Map.class).getStatusCode().value()).isEqualTo(200);
        assertThat(get(ruta,a,Map.class).getStatusCode().value()).isEqualTo(200);
        assertThat(get(ruta,b,Map.class).getStatusCode().value()).isEqualTo(403);
        assertThat(get(ruta,docenteDuenoToken,Map.class).getStatusCode().value()).isEqualTo(200);
        assertThat(post("/api/entregas/enviar",b,Map.of("tareaId",tarea,"urlArchivo",pdf.get("url")),Map.class).getStatusCode().value()).isEqualTo(403);
        var image=subir(curso,a,"ENTREGA","respuesta.png",png()).getBody();
        assertThat(post("/api/entregas/enviar",a,Map.of("tareaId",tarea,"urlArchivo",image.get("url")),Map.class).getStatusCode().value()).isEqualTo(200);
        assertThat(archivos.findById(UUID.fromString((String)image.get("id"))).orElseThrow().getProveedor()).isEqualTo("CLOUDINARY");
        var matricula=matriculaRepository.findByEstudianteId(usuarioRepository.findByCorreoElectronico("respuesta.a@it.invalid").orElseThrow().getId()).getFirst();
        delete("/api/docente/alumnos/"+matricula.getEstudiante().getId()+"/cursos/"+curso,docenteDuenoToken);
        assertThat(get(ruta,a,Map.class).getStatusCode().value()).isEqualTo(403);
    }
    @Test void anunciosAdmitenImagenOpcionalYRechazanDocumentosYArchivosDeOtroCurso(){
        UUID curso=curso();proveedor(curso);
        var sin=post("/api/anuncios",docenteDuenoToken,Map.of("cursoId",curso,"titulo","Solo texto","cuerpo","Mensaje"),Map.class);
        assertThat(sin.getStatusCode().value()).isEqualTo(201);assertThat(sin.getBody().get("imagenUrl")).isNull();
        assertThat(subir(curso,docenteDuenoToken,"ANUNCIO","no.pdf","%PDF-1.7".getBytes()).getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(r2);
        var imagen=subir(curso,docenteDuenoToken,"ANUNCIO","comunicado.png",png()).getBody();
        var con=post("/api/anuncios",docenteDuenoToken,Map.of("cursoId",curso,"titulo","Con imagen","cuerpo","Mensaje","imagenUrl",imagen.get("url")),Map.class);
        assertThat(con.getStatusCode().value()).isEqualTo(201);assertThat(con.getBody().get("imagenUrl")).isEqualTo(imagen.get("url"));
        String alumno=matricular(curso,"anuncio@it.invalid");
        assertThat(get("/api/anuncios/"+con.getBody().get("id"),alumno,Map.class).getBody().get("imagenUrl")).isEqualTo(imagen.get("url"));
        UUID otro=curso();
        assertThat(post("/api/anuncios",docenteDuenoToken,Map.of("cursoId",otro,"titulo","Ajeno","cuerpo","Mensaje","imagenUrl",imagen.get("url")),Map.class).getStatusCode().value()).isEqualTo(403);
        assertThat(put("/api/anuncios/"+con.getBody().get("id"),docenteDuenoToken,Map.of("titulo","Sin imagen","cuerpo","Mensaje","imagenUrl",""),Map.class).getBody().get("imagenUrl")).isNull();
    }

    @Test void vistaPreviaPdfRespetaPermisosYNoExponeElEnlaceR2(){
        UUID curso=curso();proveedor(curso);
        byte[] pdf="%PDF-1.7\ncontenido privado".getBytes(StandardCharsets.UTF_8);
        var archivo=subir(curso,docenteDuenoToken,"ACTIVIDAD","guía.pdf",pdf).getBody();
        String ruta="/api/cursos/"+curso+"/archivos/"+archivo.get("id")+"/vista-previa";
        String alumno=matricular(curso,"preview@it.invalid");
        assertThat(get(ruta,alumno,byte[].class).getStatusCode().value()).isEqualTo(403);
        assertThat(get(ruta,docenteAjenoToken,byte[].class).getStatusCode().value()).isEqualTo(403);
        verify(r2,never()).read(anyString());
        when(r2.read(anyString())).thenReturn(pdf);
        var preview=get(ruta,docenteDuenoToken,byte[].class);
        assertThat(preview.getStatusCode().value()).isEqualTo(200);
        assertThat(preview.getBody()).isEqualTo(pdf);
        assertThat(preview.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(preview.getHeaders().getCacheControl()).contains("no-store");
        assertThat(preview.getHeaders().getContentDisposition().getType()).isEqualTo("inline");
        assertThat(preview.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        verifyNoInteractions(office);
        verify(r2,never()).downloadUrl(anyString());
        UUID semana=semana(curso);
        post("/api/cursos/"+curso+"/contenido/TASK",docenteDuenoToken,Map.of("semanaId",semana,"titulo","Con vista previa","urlExterno",archivo.get("url"),"puntajeMaximo",20,"publicado",true,"visible",true),Map.class);
        assertThat(get(ruta,alumno,byte[].class).getStatusCode().value()).isEqualTo(200);
    }
    @Test void convierteOfficeSinCambiarOriginalYPermiteDescargarSiElConversorFalla(){
        UUID curso=curso();proveedor(curso);
        var archivo=new Archivo();archivo.setCursoId(curso);archivo.setUsuarioId(docenteDuenoId);
        archivo.setDestino("MATERIAL");archivo.setNombre("informe.docx");archivo.setMime("application/vnd.openxmlformats-officedocument.wordprocessingml.document");archivo.setBytes(8L);
        archivo.setPublicId("office/original.docx");archivo.setResourceType("raw");archivo.setProveedor("R2");archivo.setUrl("http://localhost:8080/documento");
        archivo=archivos.save(archivo);
        byte[] original=new byte[]{80,75,3,4},pdf="%PDF-1.7\nvista".getBytes();
        when(r2.read(archivo.getPublicId())).thenReturn(original);
        when(office.toPdf(original,"informe.docx")).thenReturn(pdf);
        String ruta="/api/cursos/"+curso+"/archivos/"+archivo.getId();
        var preview=get(ruta+"/vista-previa",docenteDuenoToken,byte[].class);
        assertThat(preview.getStatusCode().value()).isEqualTo(200);assertThat(preview.getBody()).isEqualTo(pdf);
        assertThat(preview.getHeaders().getContentDisposition().getFilename()).isEqualTo("informe.pdf");
        when(office.toPdf(original,"informe.docx")).thenThrow(new com.kellyacademy.shared.exception.BusinessException("CONVERSION_FALLIDA","No se pudo convertir"));
        assertThat(get(ruta+"/vista-previa",docenteDuenoToken,Map.class).getStatusCode().value()).isEqualTo(400);
        assertThat(get(ruta+"/acceso",docenteDuenoToken,Map.class).getStatusCode().value()).isEqualTo(200);
        assertThat(archivos.findById(archivo.getId()).orElseThrow().getPublicId()).isEqualTo("office/original.docx");
    }
}
