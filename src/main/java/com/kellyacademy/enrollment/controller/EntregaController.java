package com.kellyacademy.enrollment.controller;

import com.kellyacademy.enrollment.dto.request.ActualizarEntregaRequest;
import com.kellyacademy.enrollment.dto.request.CrearEntregaRequest;
import com.kellyacademy.enrollment.dto.response.EntregaResponse;
import com.kellyacademy.enrollment.dto.response.EntregaResumenResponse;
import com.kellyacademy.enrollment.enums.EstadoEntrega;
import com.kellyacademy.enrollment.service.EntregaService;
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
@RequestMapping("/api/entregas")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class EntregaController {

    private final EntregaService entregaService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('DOCENTE')")
    public ResponseEntity<Page<EntregaResumenResponse>> listar(
            @RequestParam(required = false) UUID tareaId,
            @RequestParam(required = false) UUID estudianteId,
            @RequestParam(required = false) EstadoEntrega estado,
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable
    ) {
        return ResponseEntity.ok(entregaService.listar(tareaId, estudianteId, estado, pageable));
    }

    @GetMapping("/mis-entregas")
    public ResponseEntity<Page<EntregaResumenResponse>> listarMisEntregas(
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable
    ) {
        return ResponseEntity.ok(entregaService.listarMisEntregas(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntregaResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(entregaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('DOCENTE')")
    public ResponseEntity<EntregaResponse> crear(@Valid @RequestBody CrearEntregaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entregaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ESTUDIANTE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<EntregaResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarEntregaRequest request
    ) {
        return ResponseEntity.ok(entregaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        entregaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}