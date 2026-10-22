package com.kellyacademy.communication.entity;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.shared.base.BaseEntity;
import com.kellyacademy.user.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "anuncios")
public class Anuncio extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String cuerpo;

    @Column(name="imagen_url", length=500)
    private String imagenUrl;

    @Column(nullable = false)
    private Boolean activo = true;
    @Column(nullable = false)
    private Boolean publicado = true;
    @Column(nullable = false)
    private Boolean fijado = false;
    @Column(name = "publicado_at")
    private java.time.LocalDateTime publicadoAt;
}
