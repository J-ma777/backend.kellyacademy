package com.kellyacademy.storage;

import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArchivoBibliotecaService {
    private final ArchivoBibliotecaRepository archivos;
    private final CloudinaryGateway cloudinary;
    private final R2Gateway r2;

    @Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;

    public record Resultado(UUID id, String nombre, String mime, long bytes, String url) {}

    @Transactional
    public Resultado subir(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 20L * 1024 * 1024)
            throw new BusinessException("ARCHIVO_TAMANO", "Selecciona un archivo de hasta 20 MB");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("archivo").replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        if (name.length() > 255 || !name.contains("."))
            throw new BusinessException("ARCHIVO_INVALIDO", "El nombre del archivo debe incluir una extensión válida y tener hasta 255 caracteres");
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        byte[] data;
        try { data = file.getBytes(); }
        catch (IOException ex) { throw new BusinessException("ARCHIVO_INVALIDO", "No se pudo leer el archivo"); }
        String mime = ArchivoService.validar(data, extension);
        UUID namespace = UUID.randomUUID();
        ArchivoBiblioteca entity = new ArchivoBiblioteca();
        entity.setUsuarioId(SecurityUtils.getUsuarioAutenticadoId());
        entity.setNombre(name);
        entity.setMime(mime);
        entity.setBytes(data.length);
        if (mime.startsWith("image/")) {
            entity.setProveedor("CLOUDINARY");
            entity.setPublicId(cloudinary.upload(data, extension, mime, namespace).publicId());
        } else {
            entity.setProveedor("R2");
            entity.setPublicId(r2.upload(data, extension, mime, namespace));
        }
        // La URL identifica al archivo; únicamente se entrega un enlace temporal mediante la descarga auditada.
        entity.setUrl("pendiente:" + UUID.randomUUID());
        archivos.save(entity);
        entity.setUrl(publicUrl.replaceAll("/$", "") + "/api/biblioteca/archivos/" + entity.getId());
        return new Resultado(entity.getId(), name, mime, data.length, entity.getUrl());
    }

    public void validarVinculo(String url, String anterior) {
        if (url == null || url.isBlank() || Objects.equals(url, anterior)) return;
        String base = url.split("[?#]", 2)[0];
        if (base.matches("(?i).*/api/cursos/[^/]+/archivos/[^/]+(?:/.*)?$"))
            throw new BusinessException("ARCHIVO_CURSO_NO_ADMITIDO", "Sube el archivo directamente a biblioteca para compartirlo con todos");
        if (!esReferenciaBiblioteca(base)) return;
        ArchivoBiblioteca archivo = archivos.findByUrl(url).orElseThrow(() ->
                new BusinessException("ARCHIVO_INVALIDO", "Selecciona un archivo subido a la biblioteca"));
        if (!Objects.equals(archivo.getUsuarioId(), SecurityUtils.getUsuarioAutenticadoId()))
            throw new AccessDeniedException("Solo puedes vincular un archivo que subiste");
    }

    public String urlDescarga(String url) {
        if (url == null || !esReferenciaBiblioteca(url.split("[?#]", 2)[0])) return url;
        ArchivoBiblioteca archivo = archivos.findByUrl(url).orElseThrow(() ->
                new BusinessException("ARCHIVO_INVALIDO", "El archivo de biblioteca ya no está disponible"));
        return switch (archivo.getProveedor()) {
            case "R2" -> r2.downloadUrl(archivo.getPublicId());
            case "CLOUDINARY" -> cloudinary.downloadUrl(archivo.getPublicId(), archivo.getMime());
            default -> throw new BusinessException("PROVEEDOR_NO_ADMITIDO", "El proveedor del archivo no está disponible");
        };
    }

    public void validarEnlaceExterno(String url, String anterior) {
        if (url == null || url.isBlank() || Objects.equals(url, anterior)) return;
        String base = url.split("[?#]", 2)[0];
        if (esReferenciaBiblioteca(base) || base.matches("(?i).*/api/cursos/[^/]+/archivos/[^/]+(?:/.*)?$"))
            throw new BusinessException("ARCHIVO_INVALIDO", "Usa el campo de archivo para una subida de biblioteca");
    }

    private boolean esReferenciaBiblioteca(String url) {
        return url.matches("(?i).*/api/biblioteca/archivos/[^/]+(?:/.*)?$");
    }
}
