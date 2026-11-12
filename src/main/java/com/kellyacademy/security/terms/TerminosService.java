package com.kellyacademy.security.terms;

import com.kellyacademy.shared.config.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TerminosService {
    /** Debe coincidir con el texto presentado por el frontend; cambiar al modificarlo. */
    public static final String VERSION = "2026-11-11.1";
    private final AceptacionTerminosRepository aceptaciones;

    public record Estado(String version, boolean aceptado, List<String> roles, LocalDateTime aceptadoEn) {}

    private List<String> roles(Authentication auth) {
        return auth.getAuthorities().stream().map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).sorted().distinct().toList();
    }

    @Transactional(readOnly = true)
    public Estado estado(UUID usuarioId, Authentication auth) {
        List<String> roles = roles(auth);
        String rolesGuardados = String.join(",", roles);
        var ultima = aceptaciones.findFirstByUsuarioIdOrderByAceptadoEnDescIdDesc(usuarioId);
        boolean vigente = ultima.isPresent() && VERSION.equals(ultima.get().getVersion())
                && rolesGuardados.equals(ultima.get().getRoles());
        return new Estado(VERSION, vigente, roles, vigente ? ultima.get().getAceptadoEn() : null);
    }

    @Transactional
    public Estado aceptar(UUID usuarioId, Authentication auth) {
        Estado estado = estado(usuarioId, auth);
        if (estado.aceptado()) return estado;
        AceptacionTerminos registro = new AceptacionTerminos();
        registro.setUsuarioId(usuarioId);
        registro.setVersion(VERSION);
        registro.setRoles(String.join(",", estado.roles()));
        registro.setAceptadoEn(LocalDateTime.now(AppTime.ZONA_NEGOCIO));
        aceptaciones.save(registro);
        return new Estado(VERSION, true, estado.roles(), registro.getAceptadoEn());
    }
}
