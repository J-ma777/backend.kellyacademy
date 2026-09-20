package com.kellyacademy.course.controller;

import com.kellyacademy.course.dto.request.ActualizarTareaRequest;
import com.kellyacademy.course.dto.request.CrearTareaRequest;
import com.kellyacademy.course.dto.response.TareaResponse;
import com.kellyacademy.course.dto.response.TareaResumenResponse;
import com.kellyacademy.course.service.TareaService;
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
@RequestMapping("/api/tareas")
@RequiredArgsConstructor
public class TareaController {

    private final TareaService tareaService;

    @GetMapping
    public ResponseEntity<List<TareaResumenResponse>> listar(
            @RequestParam(required = false) UUID semanaId
    ) {
        return ResponseEntity.ok(tareaService.listar(semanaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TareaResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(tareaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<TareaResponse> crear(
            @Valid @RequestBody CrearTareaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tareaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TareaResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarTareaRequest request
    ) {
        return ResponseEntity.ok(tareaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        tareaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}