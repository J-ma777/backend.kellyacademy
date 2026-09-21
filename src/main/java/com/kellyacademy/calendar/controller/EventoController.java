package com.kellyacademy.calendar.controller;

import com.kellyacademy.calendar.dto.request.ActualizarEventoRequest;
import com.kellyacademy.calendar.dto.request.CrearEventoRequest;
import com.kellyacademy.calendar.dto.response.EventoResponse;
import com.kellyacademy.calendar.dto.response.EventoResumenResponse;
import com.kellyacademy.calendar.enums.TipoEvento;
import com.kellyacademy.calendar.service.EventoService;
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
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class EventoController {

    private final EventoService eventoService;

    @GetMapping
    public ResponseEntity<Page<EventoResumenResponse>> listar(
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) UUID cursoId,
            @RequestParam(required = false) TipoEvento tipo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @PageableDefault(size = 20, sort = "inicio") Pageable pageable
    ) {
        return ResponseEntity.ok(
                eventoService.listar(usuarioId, cursoId, tipo, desde, hasta, pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(eventoService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<EventoResponse> crear(@Valid @RequestBody CrearEventoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarEventoRequest request
    ) {
        return ResponseEntity.ok(eventoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        eventoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}