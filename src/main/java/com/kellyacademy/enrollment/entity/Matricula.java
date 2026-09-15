package com.kellyacademy.enrollment.entity;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.shared.base.BaseEntity;
import com.kellyacademy.user.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "matriculas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_matriculas_curso_estudiante",
                columnNames = {"curso_id", "estudiante_id"}
        )
)
public class Matricula extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoMatricula estado;

    @Column(name = "nota_final", precision = 4, scale = 2)
    private BigDecimal notaFinal;

    @Column(name = "asistencia_porcentaje", precision = 5, scale = 2)
    private BigDecimal asistenciaPorcentaje;

    @Column(name = "matriculado_at", nullable = false)
    private LocalDateTime matriculadoAt;
}