package com.kellyacademy.calendar.controller;

import com.kellyacademy.calendar.dto.request.ActualizarDisponibilidadRequest;
import com.kellyacademy.calendar.dto.request.CrearDisponibilidadRequest;
import com.kellyacademy.calendar.dto.response.DisponibilidadResponse;
import com.kellyacademy.calendar.dto.response.DisponibilidadResumenResponse;
import com.kellyacademy.calendar.service.DisponibilidadTutoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.UUID;

@RestController
@RequestMapping("/api/disponibilidades")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DisponibilidadTutoriaController {

    private final DisponibilidadTutoriaService disponibilidadService;

    @GetMapping
    public ResponseEntity<Page<DisponibilidadResumenResponse>> listar(
            @RequestParam(required = false) UUID docenteId,
            @RequestParam(required = false) DayOfWeek diaSemana,
            @RequestParam(required = false) Boolean bloqueada,
            @PageableDefault(size = 20, sort = "diaSemana") Pageable pageable) {
        return ResponseEntity.ok(disponibilidadService.listar(docenteId, diaSemana, bloqueada, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisponibilidadResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(disponibilidadService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")
    public ResponseEntity<DisponibilidadResponse> crear(@Valid @RequestBody CrearDisponibilidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(disponibilidadService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")
    public ResponseEntity<DisponibilidadResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarDisponibilidadRequest request) {
        return ResponseEntity.ok(disponibilidadService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        disponibilidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}