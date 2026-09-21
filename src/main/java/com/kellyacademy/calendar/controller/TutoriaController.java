package com.kellyacademy.calendar.controller;

import com.kellyacademy.calendar.dto.request.ActualizarTutoriaRequest;
import com.kellyacademy.calendar.dto.request.CrearTutoriaRequest;
import com.kellyacademy.calendar.dto.response.TutoriaResponse;
import com.kellyacademy.calendar.dto.response.TutoriaResumenResponse;
import com.kellyacademy.calendar.enums.EstadoTutoria;
import com.kellyacademy.calendar.service.TutoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/tutorias")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TutoriaController {

    private final TutoriaService tutoriaService;

    @GetMapping
    public ResponseEntity<Page<TutoriaResumenResponse>> listar(
            @RequestParam(required = false) UUID cursoId,
            @RequestParam(required = false) EstadoTutoria estado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @PageableDefault(size = 20, sort = "fecha") Pageable pageable) {
        return ResponseEntity.ok(tutoriaService.listar(cursoId, estado, desde, hasta, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TutoriaResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(tutoriaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<TutoriaResponse> crear(@Valid @RequestBody CrearTutoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tutoriaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TutoriaResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarTutoriaRequest request) {
        return ResponseEntity.ok(tutoriaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        tutoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}