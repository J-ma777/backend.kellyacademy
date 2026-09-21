package com.kellyacademy.communication.controller;

import com.kellyacademy.communication.dto.response.NotificacionResponse;
import com.kellyacademy.communication.dto.response.NotificacionResumenResponse;
import com.kellyacademy.communication.enums.TipoNotificacion;
import com.kellyacademy.communication.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<Page<NotificacionResumenResponse>> listar(
            @RequestParam(required = false) Boolean leida,
            @RequestParam(required = false) TipoNotificacion tipo,
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable) {
        return ResponseEntity.ok(notificacionService.listar(leida, tipo, pageable));
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<Page<NotificacionResumenResponse>> listarNoLeidas(
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable) {
        return ResponseEntity.ok(notificacionService.listarNoLeidas(pageable));
    }

    @GetMapping("/count-no-leidas")
    public ResponseEntity<Map<String, Long>> contarNoLeidas() {
        long total = notificacionService.contarNoLeidas();
        return ResponseEntity.ok(Map.of("total", total));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificacionResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(notificacionService.obtener(id));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<NotificacionResponse> marcarLeida(@PathVariable UUID id) {
        return ResponseEntity.ok(notificacionService.marcarLeida(id));
    }

    @PatchMapping("/leer-todas")
    public ResponseEntity<Void> marcarTodasLeidas() {
        notificacionService.marcarTodasLeidas();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        notificacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}