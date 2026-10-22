package com.kellyacademy.storage;

import com.kellyacademy.course.repository.MaterialRepository;
import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.course.service.AccesoCursoService;
import com.kellyacademy.shared.exception.*;
import com.kellyacademy.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ArchivoAccesoService {
    private final ArchivoRepository archivos;
    private final AccesoCursoService acceso;
    private final MaterialRepository materiales;
    private final TareaRepository tareas;
    private final R2Gateway r2;
    private final OfficePreviewGateway office;
    public record Enlace(String url,String nombre,String mime,long bytes,int expiraEnSegundos) {}
    private Archivo autorizar(UUID cursoId,UUID archivoId) {
        var curso=acceso.obtener(cursoId);
        var archivo=archivos.findById(archivoId).filter(a->a.getCursoId().equals(cursoId))
                .orElseThrow(()->new ResourceNotFoundException("Archivo","id",archivoId));
        boolean propio=archivo.getUsuarioId().equals(SecurityUtils.getUsuarioAutenticadoId());
        if(!acceso.esGestor(curso)&&!propio){
            boolean publicado=!"ENTREGA".equals(archivo.getDestino())&&(
                    materiales.existsByUrlArchivoAndSemanaUnidadCursoIdAndPublicadoTrueAndVisibleTrue(archivo.getUrl(),cursoId)
                    ||tareas.existsByInstruccionesUrlAndSemanaUnidadCursoIdAndPublicadoTrueAndVisibleTrue(archivo.getUrl(),cursoId));
            if(!publicado)throw new AccessDeniedException("El archivo no está disponible para este usuario");
        }
        return archivo;
    }
    public Enlace obtener(UUID cursoId,UUID archivoId) {
        var archivo=autorizar(cursoId,archivoId);
        boolean privado="R2".equals(archivo.getProveedor());
        return new Enlace(privado?r2.downloadUrl(archivo.getPublicId()):archivo.getUrl(),archivo.getNombre(),archivo.getMime(),archivo.getBytes(),privado?300:0);
    }
    public record VistaPrevia(String nombre,byte[] contenido) {}
    public VistaPrevia vistaPrevia(UUID cursoId,UUID archivoId) {
        var archivo=autorizar(cursoId,archivoId);
        if(!"R2".equals(archivo.getProveedor()))throw new BusinessException("VISTA_PREVIA_NO_ADMITIDA","Las imágenes se consultan desde su visor de imagen");
        byte[] original=r2.read(archivo.getPublicId());
        boolean pdf="application/pdf".equals(archivo.getMime());
        return new VistaPrevia(archivo.getNombre().replaceFirst("\\.[^.]+$", "")+".pdf",pdf?original:office.toPdf(original,archivo.getNombre()));
    }
    public void validarVinculo(String url,UUID cursoId,ArchivoService.Destino destino) {
        if(url==null||url.isBlank())return;
        String normalized=url.trim().split("[?#]",2)[0];
        var encontrado=archivos.findByUrl(normalized);
        if(encontrado.isEmpty()){
            if(destino==ArchivoService.Destino.ANUNCIO||normalized.matches(".*/api/cursos/[^/]+/archivos/[^/]+$"))
                throw new BusinessException("ARCHIVO_INVALIDO","Selecciona un archivo subido a este curso");
            return; // Se mantienen los enlaces externos ya admitidos por la plataforma.
        }
        var archivo=encontrado.get();
        if(!archivo.getCursoId().equals(cursoId))throw new AccessDeniedException("El archivo pertenece a otro curso");
        if(destino==ArchivoService.Destino.ENTREGA){
            if(!"ENTREGA".equals(archivo.getDestino()))throw new BusinessException("ARCHIVO_INVALIDO","Sube tu propio archivo de respuesta");
            var curso=acceso.obtener(cursoId);
            if(!acceso.esGestor(curso)&&!archivo.getUsuarioId().equals(SecurityUtils.getUsuarioAutenticadoId()))
                throw new AccessDeniedException("No puedes enviar el archivo de otro alumno");
        }else if("ENTREGA".equals(archivo.getDestino()))throw new BusinessException("ARCHIVO_INVALIDO","Las entregas de alumnos no pueden utilizarse como adjuntos del curso");
        if(destino==ArchivoService.Destino.ANUNCIO&&(!archivo.getMime().startsWith("image/")||!"CLOUDINARY".equals(archivo.getProveedor())))
            throw new BusinessException("IMAGEN_REQUERIDA","El anuncio admite una imagen opcional subida a Cloudinary");
    }
}
