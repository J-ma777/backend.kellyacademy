package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.GuardarContenidoRequest;
import com.kellyacademy.course.dto.response.PanelCursoResponse;
import com.kellyacademy.course.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/cursos/{cursoId}")
@RequiredArgsConstructor
public class AulaCursoController {
    private final PanelCursoService panel;
    private final ContenidoCursoService contenido;
    public record Publicacion(@NotNull Boolean publicado, @NotNull Boolean visible) {}
    @GetMapping("/aula")
    public PanelCursoResponse aula(@PathVariable UUID cursoId) { return panel.obtenerAula(cursoId); }
    @PostMapping("/contenido/{tipo}")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMINISTRADOR')")
    public ResponseEntity<Void> crear(@PathVariable UUID cursoId, @PathVariable String tipo, @Valid @RequestBody GuardarContenidoRequest request) {
        contenido.guardar(cursoId, tipo, null, request);
        return ResponseEntity.status(201).build();
    }
    @PutMapping("/contenido/{tipo}/{id}")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMINISTRADOR')")
    public ResponseEntity<Void> actualizar(@PathVariable UUID cursoId, @PathVariable String tipo, @PathVariable UUID id, @Valid @RequestBody GuardarContenidoRequest request) {
        contenido.guardar(cursoId, tipo, id, request);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/contenido/{tipo}/{id}/publicacion")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMINISTRADOR')")
    public ResponseEntity<Void> publicar(@PathVariable UUID cursoId, @PathVariable String tipo, @PathVariable UUID id, @Valid @RequestBody Publicacion request) {
        contenido.publicar(cursoId, tipo, id, request.publicado(), request.visible());
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/contenido/{tipo}/{id}/completar")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Void> completar(@PathVariable UUID cursoId, @PathVariable String tipo, @PathVariable UUID id) {
        contenido.completar(cursoId, tipo, id);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/alumnos/{estudianteId}")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMINISTRADOR')")
    public ResponseEntity<Void> retirar(@PathVariable UUID cursoId, @PathVariable UUID estudianteId) {
        contenido.retirarAlumno(cursoId, estudianteId);
        return ResponseEntity.noContent().build();
    }
}
