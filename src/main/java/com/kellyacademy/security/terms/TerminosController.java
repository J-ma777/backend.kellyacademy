package com.kellyacademy.security.terms;

import com.kellyacademy.shared.config.AppTime;
import com.kellyacademy.shared.exception.ErrorResponse;
import com.kellyacademy.shared.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/terminos/aceptacion")
@RequiredArgsConstructor
public class TerminosController {
    private final TerminosService terminos;
    public record Solicitud(@NotBlank String version, @NotNull List<@NotBlank String> rolesMostrados) {}

    @GetMapping
    public ResponseEntity<TerminosService.Estado> estado(Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(terminos.estado(SecurityUtils.getUsuarioAutenticadoId(), auth));
    }

    @PostMapping
    public ResponseEntity<?> aceptar(@Valid @RequestBody Solicitud solicitud, Authentication auth, HttpServletRequest request) {
        var estado = terminos.estado(SecurityUtils.getUsuarioAutenticadoId(), auth);
        var rolesMostrados = solicitud.rolesMostrados().stream().distinct().sorted().toList();
        if (!TerminosService.VERSION.equals(solicitud.version()) || !estado.roles().equals(rolesMostrados)) {
            ErrorResponse error = ErrorResponse.builder().codigo("TERMINOS_VERSION_DESACTUALIZADA")
                    .mensaje("Los términos han cambiado. Actualiza la página y revísalos nuevamente.")
                    .estado(HttpStatus.CONFLICT.value()).ruta(request.getRequestURI())
                    .timestamp(LocalDateTime.now(AppTime.ZONA_NEGOCIO)).build();
            return ResponseEntity.status(HttpStatus.CONFLICT).cacheControl(CacheControl.noStore()).body(error);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(terminos.aceptar(SecurityUtils.getUsuarioAutenticadoId(), auth));
    }
}
