package com.kellyacademy.security.terms;

import com.kellyacademy.shared.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Evidencia de la versión y los perfiles aceptados en una sesión autenticada. */
@Entity
@Table(name = "aceptaciones_terminos")
@Getter @Setter @NoArgsConstructor
public class AceptacionTerminos extends BaseEntity {
    @Column(nullable = false) private UUID usuarioId;
    @Column(nullable = false, length = 40) private String version;
    @Column(nullable = false, length = 500) private String roles;
    @Column(nullable = false) private LocalDateTime aceptadoEn;
}
