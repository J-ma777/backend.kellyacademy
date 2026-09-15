package com.kellyacademy.course.entity;

import com.kellyacademy.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tareas")
public class Tarea extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semana_id", nullable = false)
    private Semana semana;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "instrucciones_url", length = 500)
    private String instruccionesUrl;

    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;

    @Column(name = "puntaje_maximo", nullable = false)
    private Integer puntajeMaximo = 100;
}