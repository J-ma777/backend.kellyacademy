package com.kellyacademy.enrollment.controller;

import com.kellyacademy.enrollment.dto.request.CrearMatriculaRequest;
import com.kellyacademy.enrollment.dto.response.MatriculaResponse;
import com.kellyacademy.enrollment.dto.response.MatriculaResumenResponse;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.service.MatriculaService;
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
@RequestMapping("/api/matriculas")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MatriculaController {

    private final MatriculaService matriculaService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<MatriculaResumenResponse>> listar(
            @RequestParam(required = false) UUID cursoId,
            @RequestParam(required = false) UUID estudianteId,
            @RequestParam(required = false) EstadoMatricula estado,
            @PageableDefault(size = 20, sort = "matriculadoAt") Pageable pageable
    ) {
        return ResponseEntity.ok(matriculaService.listar(cursoId, estudianteId, estado, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatriculaResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(matriculaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MatriculaResponse> crear(@Valid @RequestBody CrearMatriculaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matriculaService.crear(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        matriculaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}