package com.kellyacademy.calendar.entity;

import com.kellyacademy.calendar.enums.TipoEvento;
import com.kellyacademy.course.entity.Curso;
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
@Table(name = "eventos")
public class Evento extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoEvento tipo;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column
    private LocalDateTime fin;

    @Column(length = 100)
    private String sala;
}