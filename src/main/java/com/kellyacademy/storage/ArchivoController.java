package com.kellyacademy.storage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
@RestController @RequestMapping("/api/cursos/{cursoId}/archivos") @RequiredArgsConstructor
public class ArchivoController {
    private final ArchivoService archivos;
    private final ArchivoAccesoService acceso;
    @GetMapping("/{archivoId}/acceso")
    public ResponseEntity<ArchivoAccesoService.Enlace> acceso(@PathVariable UUID cursoId,@PathVariable UUID archivoId){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(acceso.obtener(cursoId,archivoId));
    }
    @GetMapping("/{archivoId}/vista-previa")
    public ResponseEntity<byte[]> vistaPrevia(@PathVariable UUID cursoId,@PathVariable UUID archivoId){
        var documento=acceso.vistaPrevia(cursoId,archivoId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options","nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.inline().filename(documento.nombre(),java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(documento.contenido());
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArchivoService.Resultado> subir(@PathVariable UUID cursoId,
            @RequestParam ArchivoService.Destino destino,@RequestPart("archivo") MultipartFile archivo){
        return ResponseEntity.status(HttpStatus.CREATED).body(archivos.subir(cursoId,destino,archivo));
    }
}
