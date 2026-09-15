package com.kellyacademy.course.entity;

import com.kellyacademy.course.enums.TipoMaterial;
import com.kellyacademy.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "materiales")
public class Material extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semana_id", nullable = false)
    private Semana semana;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "url_archivo", length = 500)
    private String urlArchivo;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TipoMaterial tipo;

    @Column(name = "tamano_mb", precision = 10, scale = 2)
    private BigDecimal tamanoMb;

    @Column(name = "url_externo", length = 500)
    private String urlExterno;
}