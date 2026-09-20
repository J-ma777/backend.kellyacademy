package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.ActualizarSemanaRequest;
import com.kellyacademy.course.dto.request.CrearSemanaRequest;
import com.kellyacademy.course.dto.response.SemanaResponse;
import com.kellyacademy.course.dto.response.SemanaResumenResponse;
import com.kellyacademy.course.service.SemanaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/semanas")
@RequiredArgsConstructor
public class SemanaController {

    private final SemanaService semanaService;

    // Listado por unidad: sin paginacion, las semanas de una unidad son pocas (4-8 tipico).
    @GetMapping
    public ResponseEntity<List<SemanaResumenResponse>> listar(
            @RequestParam UUID unidadId
    ) {
        return ResponseEntity.ok(semanaService.listar(unidadId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SemanaResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(semanaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<SemanaResponse> crear(
            @Valid @RequestBody CrearSemanaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(semanaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SemanaResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarSemanaRequest request
    ) {
        return ResponseEntity.ok(semanaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        semanaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // Marca la semana como actual y desmarca la anterior de la misma unidad.
    // Idempotente: si ya es actual, devuelve 200 sin cambios.
    @PatchMapping("/{id}/marcar-actual")
    public ResponseEntity<SemanaResponse> marcarActual(@PathVariable UUID id) {
        return ResponseEntity.ok(semanaService.marcarActual(id));
    }
}