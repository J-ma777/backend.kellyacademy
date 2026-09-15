package com.kellyacademy.library.entity;

import com.kellyacademy.course.enums.NivelCefr;
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
@Table(name = "recursos_biblioteca")
public class RecursoBiblioteca extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 100)
    private String categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_cefr", length = 10)
    private NivelCefr nivelCefr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoMaterial tipo;

    @Column(name = "url_archivo", length = 500)
    private String urlArchivo;

    @Column(name = "url_externo", length = 500)
    private String urlExterno;

    @Column(name = "tamano_mb", precision = 10, scale = 2)
    private BigDecimal tamanoMb;

    @Column(name = "contador_descargas", nullable = false)
    private Integer contadorDescargas = 0;
}