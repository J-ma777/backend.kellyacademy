package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.ActualizarUnidadRequest;
import com.kellyacademy.course.dto.request.CrearUnidadRequest;
import com.kellyacademy.course.dto.response.UnidadResponse;
import com.kellyacademy.course.dto.response.UnidadResumenResponse;
import com.kellyacademy.course.service.UnidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/unidades")
@RequiredArgsConstructor
public class UnidadController {

    private final UnidadService unidadService;

    // Listado por curso: sin paginacion, las unidades de un curso son pocas (4-8 tipico).
    @GetMapping
    public ResponseEntity<List<UnidadResumenResponse>> listar(
            @RequestParam(required = false) UUID cursoId
    ) {
        return ResponseEntity.ok(unidadService.listar(cursoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(unidadService.obtener(id));
    }

    // Autorizacion fina (docente dueno del curso o ADMIN) en el servicio.
    @PostMapping
    public ResponseEntity<UnidadResponse> crear(
            @Valid @RequestBody CrearUnidadRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unidadService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnidadResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarUnidadRequest request
    ) {
        return ResponseEntity.ok(unidadService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        unidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}