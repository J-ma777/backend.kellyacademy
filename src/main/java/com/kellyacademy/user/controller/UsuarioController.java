package com.kellyacademy.user.controller;

import com.kellyacademy.user.dto.request.*;
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
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> obtenerActual() {
        return ResponseEntity.ok(usuarioService.obtener(com.kellyacademy.shared.util.SecurityUtils.getUsuarioAutenticadoId()));
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

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request
    ) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, request.estado()));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponse> asignarRoles(
            @PathVariable UUID id,
            @Valid @RequestBody AsignarRolesUsuarioRequest request
    ) {
        return ResponseEntity.ok(usuarioService.asignarRoles(id, request.roles()));
    }

    // Cambio de contrasena del propio usuario. La validacion de "solo el propio"
    // va en el servicio (depende del {id} del path).
    @PatchMapping("/{id}/contrasena")
    public ResponseEntity<Void> cambiarContrasena(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarContrasenaRequest request
    ) {
        usuarioService.cambiarContrasena(id, request);
        return ResponseEntity.noContent().build();
    }

    // Reset administrativo de contrasena. Solo ADMINISTRADOR.
    // Fuerza una contrasena nueva sin validar la actual (el admin no la conoce).
    // El usuario afectado recibe una notificacion de tipo SISTEMA.
    @PostMapping("/{id}/reset-contrasena")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> resetearContrasena(
            @PathVariable UUID id,
            @Valid @RequestBody ResetContrasenaRequest request
    ) {
        usuarioService.resetearContrasena(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}