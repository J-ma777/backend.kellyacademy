package com.kellyacademy.storage;

import com.kellyacademy.course.service.AccesoCursoService;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipInputStream;

@Service @RequiredArgsConstructor
public class ArchivoService {
    private final AccesoCursoService acceso;
    private final CloudinaryGateway cloudinary;
    private final R2Gateway r2;
    @org.springframework.beans.factory.annotation.Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;
    private final ArchivoRepository archivos;
    public enum Destino { MATERIAL, ACTIVIDAD, ANUNCIO, ENTREGA }
    public record Resultado(UUID id,String nombre,String mime,long bytes,String url) {}
    @Transactional
    public Resultado subir(UUID cursoId,Destino destino,MultipartFile file) {
        var curso=acceso.obtener(cursoId);
        if(destino!=Destino.ENTREGA) acceso.validarGestor(curso);
        else if(!acceso.esGestor(curso)) acceso.validarEstudianteActivo(curso);
        if(file.isEmpty()||file.getSize()>20L*1024*1024)
            throw new BusinessException("ARCHIVO_TAMANO","Selecciona un archivo de hasta 20 MB");
        String name=Optional.ofNullable(file.getOriginalFilename()).orElse("archivo").replace('\\','/');
        name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]", "");
        if(name.length()>255||!name.contains(".")) throw new BusinessException("ARCHIVO_INVALIDO","El nombre del archivo debe incluir una extensión válida y tener hasta 255 caracteres");
        String extension=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        byte[] data;
        try{data=file.getBytes();}catch(IOException ex){throw new BusinessException("ARCHIVO_INVALIDO","No se pudo leer el archivo");}
        String mime=validar(data,extension);
        boolean image=mime.startsWith("image/");
        if(destino==Destino.ANUNCIO&&!image)throw new BusinessException("IMAGEN_REQUERIDA","Los anuncios admiten una imagen opcional en JPG, PNG, GIF o WebP");
        var entity=new Archivo();entity.setCursoId(cursoId);entity.setUsuarioId(SecurityUtils.getUsuarioAutenticadoId());
        entity.setDestino(destino.name());entity.setNombre(name);entity.setMime(mime);entity.setBytes(data.length);
        if(image){
            var uploaded=cloudinary.upload(data,extension,mime,cursoId);
            entity.setProveedor("CLOUDINARY");entity.setPublicId(uploaded.publicId());
            entity.setResourceType(uploaded.resourceType());entity.setUrl(uploaded.url());
        }else{
            entity.setProveedor("R2");entity.setPublicId(r2.upload(data,extension,mime,cursoId));
            entity.setResourceType("raw");entity.setUrl("pendiente");
        }
        archivos.save(entity);
        if(!image)entity.setUrl(publicUrl.replaceAll("/$", "")+"/api/cursos/"+cursoId+"/archivos/"+entity.getId());
        return new Resultado(entity.getId(),name,mime,data.length,entity.getUrl());
    }
    static String validar(byte[] b,String extension) {
        String mime=switch(extension){
            case "jpg","jpeg" -> starts(b,0xff,0xd8,0xff)?"image/jpeg":null;
            case "png" -> starts(b,137,80,78,71,13,10,26,10)?"image/png":null;
            case "gif" -> text(b,0,"GIF87a")||text(b,0,"GIF89a")?"image/gif":null;
            case "webp" -> text(b,0,"RIFF")&&text(b,8,"WEBP")?"image/webp":null;
            case "pdf" -> text(b,0,"%PDF-")?"application/pdf":null;
            case "doc","xls","ppt" -> starts(b,208,207,17,224,161,177,26,225)?switch(extension){case "doc"->"application/msword";case "xls"->"application/vnd.ms-excel";default->"application/vnd.ms-powerpoint";}:null;
            case "docx","xlsx","pptx" -> office(b,extension)?"application/vnd.openxmlformats-officedocument."+switch(extension){case "docx"->"wordprocessingml.document";case "xlsx"->"spreadsheetml.sheet";default->"presentationml.presentation";}:null;
            case "txt" -> !new String(b,StandardCharsets.UTF_8).contains("\u0000")?"text/plain":null;
            default -> null;
        };
        if(mime==null)throw new BusinessException("FORMATO_NO_PERMITIDO","Archivo inválido. Usa JPG, PNG, GIF, WebP, PDF, Word, Excel, PowerPoint o TXT");
        return mime;
    }
    private static boolean starts(byte[] b,int... prefix){if(b.length<prefix.length)return false;for(int i=0;i<prefix.length;i++)if((b[i]&255)!=prefix[i])return false;return true;}
    private static boolean text(byte[] b,int offset,String text){return b.length>=offset+text.length()&&new String(b,offset,text.length(),StandardCharsets.US_ASCII).equals(text);}
    private static boolean office(byte[] b,String ext){
        String expected=switch(ext){case "docx"->"word/document.xml";case "xlsx"->"xl/workbook.xml";default->"ppt/presentation.xml";};
        boolean content=false,document=false;
        try(var zip=new ZipInputStream(new ByteArrayInputStream(b))){
            java.util.zip.ZipEntry entry;int entries=0;long total=0;
            // Solo inspeccionar el directorio; no extraer un ZIP arbitrario ni descomprimir entradas enormes.
            while((entry=zip.getNextEntry())!=null&&entries++<1000){
                if(entry.getName().equals("[Content_Types].xml"))content=true;
                if(entry.getName().equals(expected))document=true;
                if(content&&document)return true;
                long read=0;byte[] buffer=new byte[4096];int n;
                while((n=zip.read(buffer))!=-1){read+=n;total+=n;if(read>20L*1024*1024||total>40L*1024*1024)return false;}
            }
        }catch(IOException ex){return false;}return false;
    }
}
