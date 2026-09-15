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
        name = "unidades",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_unidades_curso_numero",
                columnNames = {"curso_id", "numero"}
        )
)
public class Unidad extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
}