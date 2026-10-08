package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.ActualizarCursoRequest;
import com.kellyacademy.course.dto.request.CambiarDocenteCursoRequest;
import com.kellyacademy.course.dto.request.CambiarEstadoCursoRequest;
import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.dto.response.CursoResumenResponse;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.service.CursoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService cursoService;

    @GetMapping
    public ResponseEntity<Page<CursoResumenResponse>> listar(
            @RequestParam(required = false) EstadoCurso estado,
            @RequestParam(required = false) NivelCefr nivelCefr,
            @RequestParam(required = false) UUID docenteId,
            @RequestParam(required = false) String titulo,
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable
    ) {
        return ResponseEntity.ok(
                cursoService.listar(estado, nivelCefr, docenteId, titulo, pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CursoResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(cursoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CursoResponse> crear(
            @Valid @RequestBody CrearCursoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cursoService.crear(request));
    }

    // Autorizacion fina (docente dueno o ADMIN) en el servicio.
    @PutMapping("/{id}")
    public ResponseEntity<CursoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarCursoRequest request
    ) {
        return ResponseEntity.ok(cursoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/docente")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CursoResponse> cambiarDocente(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarDocenteCursoRequest request
    ) {
        return ResponseEntity.ok(cursoService.cambiarDocente(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CursoResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarEstadoCursoRequest request
    ) {
        return ResponseEntity.ok(cursoService.cambiarEstado(id, request.estado()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        cursoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}