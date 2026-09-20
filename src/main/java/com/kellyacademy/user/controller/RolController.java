package com.kellyacademy.user.controller;

import com.kellyacademy.user.dto.response.RolResponse;
import com.kellyacademy.user.dto.response.RolResumenResponse;
import com.kellyacademy.user.service.RolService;
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
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RolController {

    private final RolService rolService;

    @GetMapping
    public ResponseEntity<Page<RolResumenResponse>> listar(
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable
    ) {
        return ResponseEntity.ok(rolService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(rolService.obtener(id));
    }
}