package com.kellyacademy.enrollment.controller;
import com.kellyacademy.enrollment.dto.request.EnviarEntregaRequest;
import com.kellyacademy.enrollment.dto.response.EntregaResponse;
import com.kellyacademy.enrollment.service.EnvioEstudianteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/entregas/enviar")
@RequiredArgsConstructor
public class EnvioEstudianteController {
    private final EnvioEstudianteService service;
    @PostMapping
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public EntregaResponse enviar(@Valid @RequestBody EnviarEntregaRequest request) { return service.enviar(request); }
}
