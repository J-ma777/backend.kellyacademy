package com.kellyacademy.course.entity;

import com.kellyacademy.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "semanas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_semanas_unidad_numero",
                columnNames = {"unidad_id", "numero"}
        )
)
public class Semana extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_id", nullable = false)
    private Unidad unidad;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "es_actual", nullable = false)
    private Boolean esActual = false;
}