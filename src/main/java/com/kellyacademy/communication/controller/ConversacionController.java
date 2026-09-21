package com.kellyacademy.communication.controller;

import com.kellyacademy.communication.dto.request.CrearConversacionRequest;
import com.kellyacademy.communication.dto.response.ConversacionResponse;
import com.kellyacademy.communication.dto.response.ConversacionResumenResponse;
import com.kellyacademy.communication.service.ConversacionService;
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
@RequestMapping("/api/conversaciones")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ConversacionController {

    private final ConversacionService conversacionService;

    @GetMapping
    public ResponseEntity<Page<ConversacionResumenResponse>> listar(
            @RequestParam(required = false) UUID cursoId,
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable) {
        return ResponseEntity.ok(conversacionService.listar(cursoId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversacionResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(conversacionService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<ConversacionResponse> crear(@Valid @RequestBody CrearConversacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conversacionService.crear(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        conversacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}