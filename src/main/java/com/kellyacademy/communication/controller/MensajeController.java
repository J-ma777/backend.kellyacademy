package com.kellyacademy.communication.controller;

import com.kellyacademy.communication.dto.request.CrearMensajeRequest;
import com.kellyacademy.communication.dto.response.MensajeResponse;
import com.kellyacademy.communication.dto.response.MensajeResumenResponse;
import com.kellyacademy.communication.service.MensajeService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MensajeController {

    private final MensajeService mensajeService;

    @GetMapping("/conversaciones/{conversacionId}/mensajes")
    public ResponseEntity<Page<MensajeResumenResponse>> listar(
            @PathVariable UUID conversacionId,
            @PageableDefault(size = 50, sort = "enviadoAt") Pageable pageable) {
        return ResponseEntity.ok(mensajeService.listar(conversacionId, pageable));
    }

    @GetMapping("/mensajes/{id}")
    public ResponseEntity<MensajeResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(mensajeService.obtener(id));
    }

    @PostMapping("/conversaciones/{conversacionId}/mensajes")
    public ResponseEntity<MensajeResponse> crear(
            @PathVariable UUID conversacionId,
            @Valid @RequestBody CrearMensajeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mensajeService.crear(conversacionId, request));
    }

    @PatchMapping("/conversaciones/{conversacionId}/mensajes/{mensajeId}/leer")
    public ResponseEntity<MensajeResponse> marcarLeido(
            @PathVariable UUID conversacionId,
            @PathVariable UUID mensajeId) {
        return ResponseEntity.ok(mensajeService.marcarLeido(conversacionId, mensajeId));
    }

    @PatchMapping("/conversaciones/{conversacionId}/leer-todos")
    public ResponseEntity<Void> marcarTodosLeidos(@PathVariable UUID conversacionId) {
        mensajeService.marcarTodosLeidos(conversacionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/mensajes/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        mensajeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}