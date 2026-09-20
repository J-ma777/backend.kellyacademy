package com.kellyacademy.user.controller;

import com.kellyacademy.user.dto.request.ActualizarUsuarioRequest;
import com.kellyacademy.user.dto.request.CrearUsuarioRequest;
import com.kellyacademy.user.dto.response.UsuarioResponse;
import com.kellyacademy.user.dto.response.UsuarioResumenResponse;
import com.kellyacademy.user.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<UsuarioResumenResponse>> listar(
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable
    ) {
        return ResponseEntity.ok(usuarioService.listar(pageable));
    }

    // Autorizacion fina (propio o admin) en el servicio: no se puede resolver con @PreAuthorize
    // de forma limpia porque depende del {id} del path.
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponse> crear(
            @Valid @RequestBody CrearUsuarioRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    // Autorizacion fina (propio o admin) en el servicio.
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarUsuarioRequest request
    ) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}