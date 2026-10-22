package com.kellyacademy.course.controller;
import com.kellyacademy.course.dto.request.GuardarHorarioCursoRequest;
import com.kellyacademy.course.service.HorarioCursoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/cursos/{id}/horarios") @RequiredArgsConstructor
public class HorarioCursoController {
    private final HorarioCursoService horarios;
    @GetMapping public HorarioCursoService.Plan obtener(@PathVariable UUID id){return horarios.obtener(id);}
    @PutMapping @PreAuthorize("hasAnyRole('DOCENTE','ADMINISTRADOR')")
    public ResponseEntity<Void> guardar(@PathVariable UUID id,@Valid @RequestBody GuardarHorarioCursoRequest r){horarios.guardar(id,r);return ResponseEntity.noContent().build();}
}
