package com.kellyacademy.course.entity;

import com.kellyacademy.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name="horarios_curso")
@Getter @Setter
public class HorarioCurso extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="curso_id",nullable=false)
    @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Curso curso;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10)
    private DayOfWeek dia;
    @Column(name="hora_inicio",nullable=false) private LocalTime horaInicio;
    @Column(name="hora_fin",nullable=false) private LocalTime horaFin;
}
