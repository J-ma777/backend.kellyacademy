package com.kellyacademy.library.controller;

import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.enums.TipoMaterial;
import com.kellyacademy.library.dto.request.ActualizarRecursoRequest;
import com.kellyacademy.library.dto.request.CrearRecursoRequest;
import com.kellyacademy.library.dto.response.RecursoResponse;
import com.kellyacademy.library.dto.response.RecursoResumenResponse;
import com.kellyacademy.library.service.RecursoBibliotecaService;
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
@RequestMapping("/api/recursos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RecursoBibliotecaController {

    private final RecursoBibliotecaService recursoService;

    @GetMapping
    public ResponseEntity<Page<RecursoResumenResponse>> listar(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) NivelCefr nivelCefr,
            @RequestParam(required = false) TipoMaterial tipo,
            @RequestParam(required = false) String titulo,
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable) {
        return ResponseEntity.ok(recursoService.listar(categoria, nivelCefr, tipo, titulo, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecursoResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(recursoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")
    public ResponseEntity<RecursoResponse> crear(@Valid @RequestBody CrearRecursoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recursoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")
    public ResponseEntity<RecursoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarRecursoRequest request) {
        return ResponseEntity.ok(recursoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        recursoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}