package com.kellyacademy.user.controller;

import com.kellyacademy.user.dto.response.PermisoResponse;
import com.kellyacademy.user.dto.response.PermisoResumenResponse;
import com.kellyacademy.user.service.PermisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/permisos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class PermisoController {

    private final PermisoService permisoService;

    @GetMapping
    public ResponseEntity<Page<PermisoResumenResponse>> listar(
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable
    ) {
        return ResponseEntity.ok(permisoService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermisoResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(permisoService.obtener(id));
    }
}