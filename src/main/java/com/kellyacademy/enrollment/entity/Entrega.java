package com.kellyacademy.enrollment.entity;

import com.kellyacademy.course.entity.Tarea;
import com.kellyacademy.enrollment.enums.EstadoEntrega;
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
        name = "entregas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_entregas_tarea_estudiante",
                columnNames = {"tarea_id", "estudiante_id"}
        )
)
public class Entrega extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tarea_id", nullable = false)
    private Tarea tarea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    @Column(name = "url_archivo", length = 500)
    private String urlArchivo;

    @Column(name = "enviado_at")
    private LocalDateTime enviadoAt;

    @Column(precision = 5, scale = 2)
    private BigDecimal nota;

    @Column(columnDefinition = "TEXT")
    private String retroalimentacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoEntrega estado;
}