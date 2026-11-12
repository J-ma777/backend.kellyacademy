package com.kellyacademy.security.terms;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AceptacionTerminosRepository extends JpaRepository<AceptacionTerminos, UUID> {
    Optional<AceptacionTerminos> findFirstByUsuarioIdOrderByAceptadoEnDescIdDesc(UUID usuarioId);
}
