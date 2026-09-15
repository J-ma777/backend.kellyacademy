package com.kellyacademy.calendar.entity;

import com.kellyacademy.calendar.enums.EstadoTutoria;
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
@Table(name = "tutorias")
public class Tutoria extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    private Usuario docente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoTutoria estado;

    @Column(columnDefinition = "TEXT")
    private String notas;
}