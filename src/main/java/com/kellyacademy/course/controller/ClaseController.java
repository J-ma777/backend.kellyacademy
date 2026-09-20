package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.ActualizarClaseRequest;
import com.kellyacademy.course.dto.request.CrearClaseRequest;
import com.kellyacademy.course.dto.response.ClaseResponse;
import com.kellyacademy.course.dto.response.ClaseResumenResponse;
import com.kellyacademy.course.service.ClaseService;
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
@RequestMapping("/api/clases")
@RequiredArgsConstructor
public class ClaseController {

    private final ClaseService claseService;

    @GetMapping
    public ResponseEntity<List<ClaseResumenResponse>> listar(
            @RequestParam(required = false) UUID semanaId
    ) {
        return ResponseEntity.ok(claseService.listar(semanaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaseResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(claseService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<ClaseResponse> crear(
            @Valid @RequestBody CrearClaseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claseService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClaseResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarClaseRequest request
    ) {
        return ResponseEntity.ok(claseService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        claseService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}