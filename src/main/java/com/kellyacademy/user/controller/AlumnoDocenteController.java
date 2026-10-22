package com.kellyacademy.user.controller;

import com.kellyacademy.user.dto.alumno.AlumnoDtos.*;
import com.kellyacademy.user.service.AlumnoDocenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/docente/alumnos")
@PreAuthorize("hasRole('DOCENTE')")
@RequiredArgsConstructor
public class AlumnoDocenteController {
    private final AlumnoDocenteService alumnos;
    @GetMapping public Directorio listar(){ return alumnos.listar(); }
    @PostMapping public ResponseEntity<Alumno> crear(@Valid @RequestBody Crear r){ return ResponseEntity.status(201).body(alumnos.crear(r)); }
    @PatchMapping("/{id}") public Alumno editar(@PathVariable UUID id,@Valid @RequestBody Actualizar r){ return alumnos.actualizar(id,r); }
    @PatchMapping("/{id}/estado") public Alumno estado(@PathVariable UUID id,@Valid @RequestBody CambiarEstado r){ return alumnos.cambiarEstado(id,r.estado()); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable UUID id){ alumnos.eliminar(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/cursos") public ResponseEntity<Void> inscribir(@PathVariable UUID id,@Valid @RequestBody Inscribir r){ alumnos.inscribir(id,r.cursoId()); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/{id}/cursos/{cursoId}") public ResponseEntity<Void> retirar(@PathVariable UUID id,@PathVariable UUID cursoId){ alumnos.retirar(id,cursoId); return ResponseEntity.noContent().build(); }
}
