package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.ActualizarMaterialRequest;
import com.kellyacademy.course.dto.request.CrearMaterialRequest;
import com.kellyacademy.course.dto.response.MaterialResponse;
import com.kellyacademy.course.dto.response.MaterialResumenResponse;
import com.kellyacademy.course.service.MaterialService;
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
@RequestMapping("/api/materiales")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public ResponseEntity<List<MaterialResumenResponse>> listar(
            @RequestParam(required = false) UUID semanaId
    ) {
        return ResponseEntity.ok(materialService.listar(semanaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(materialService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<MaterialResponse> crear(
            @Valid @RequestBody CrearMaterialRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarMaterialRequest request
    ) {
        return ResponseEntity.ok(materialService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        materialService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}