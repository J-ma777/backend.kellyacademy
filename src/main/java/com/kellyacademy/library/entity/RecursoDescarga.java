package com.kellyacademy.library.entity;

import com.kellyacademy.shared.base.BaseEntity;
import com.kellyacademy.user.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "recurso_descargas")
public class RecursoDescarga extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_id", nullable = false)
    private RecursoBiblioteca recurso;

    // Nullable: ON DELETE SET NULL si se borra el usuario.
    // La traza sobrevive de forma anonima.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 45)
    private String ip;

    @Column(name = "fecha_descarga", nullable = false)
    private LocalDateTime fechaDescarga;
}