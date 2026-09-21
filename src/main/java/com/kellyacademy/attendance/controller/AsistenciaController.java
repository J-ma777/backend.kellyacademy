package com.kellyacademy.attendance.controller;

import com.kellyacademy.attendance.dto.request.ActualizarAsistenciaRequest;
import com.kellyacademy.attendance.dto.request.CrearAsistenciaRequest;
import com.kellyacademy.attendance.dto.response.AsistenciaResponse;
import com.kellyacademy.attendance.dto.response.AsistenciaResumenResponse;
import com.kellyacademy.attendance.service.AsistenciaService;
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
@RequestMapping("/api/asistencias")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")
    public ResponseEntity<Page<AsistenciaResumenResponse>> listar(
            @RequestParam(required = false) UUID claseId,
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable) {
        return ResponseEntity.ok(asistenciaService.listar(claseId, pageable));
    }

    @GetMapping("/mis-asistencias")
    public ResponseEntity<Page<AsistenciaResumenResponse>> listarMisAsistencias(
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable) {
        return ResponseEntity.ok(asistenciaService.listarMisAsistencias(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AsistenciaResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(asistenciaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")
    public ResponseEntity<AsistenciaResponse> crear(@Valid @RequestBody CrearAsistenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asistenciaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")
    public ResponseEntity<AsistenciaResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarAsistenciaRequest request) {
        return ResponseEntity.ok(asistenciaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        asistenciaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}