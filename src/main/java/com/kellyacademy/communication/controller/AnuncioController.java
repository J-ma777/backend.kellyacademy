package com.kellyacademy.communication.controller;

import com.kellyacademy.communication.dto.request.ActualizarAnuncioRequest;
import com.kellyacademy.communication.dto.request.CrearAnuncioRequest;
import com.kellyacademy.communication.dto.response.AnuncioResponse;
import com.kellyacademy.communication.dto.response.AnuncioResumenResponse;
import com.kellyacademy.communication.service.AnuncioService;
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
@RequestMapping("/api/anuncios")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AnuncioController {

    private final AnuncioService anuncioService;

    @GetMapping
    public ResponseEntity<Page<AnuncioResumenResponse>> listar(
            @RequestParam(required = false) UUID cursoId,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable) {
        return ResponseEntity.ok(anuncioService.listar(cursoId, activo, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnuncioResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(anuncioService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")
    public ResponseEntity<AnuncioResponse> crear(@Valid @RequestBody CrearAnuncioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(anuncioService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")
    public ResponseEntity<AnuncioResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarAnuncioRequest request) {
        return ResponseEntity.ok(anuncioService.actualizar(id, request));
    }

    @PatchMapping("/{id}/archivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")
    public ResponseEntity<AnuncioResponse> archivar(@PathVariable UUID id) {
        return ResponseEntity.ok(anuncioService.archivar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        anuncioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}