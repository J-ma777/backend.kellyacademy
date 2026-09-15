package com.kellyacademy.attendance.entity;

import com.kellyacademy.attendance.enums.EstadoAsistencia;
import com.kellyacademy.course.entity.Clase;
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
@Table(
        name = "asistencias",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_asistencias_clase_estudiante",
                columnNames = {"clase_id", "estudiante_id"}
        )
)
public class Asistencia extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clase_id", nullable = false)
    private Clase clase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoAsistencia estado;

    @Column(length = 500)
    private String observacion;

    @Column(name = "registrado_at", nullable = false)
    private LocalDateTime registradoAt;
}