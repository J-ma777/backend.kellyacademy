package com.kellyacademy.communication.entity;

import com.kellyacademy.communication.enums.TipoNotificacion;
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
@Table(name = "notificaciones")
public class Notificacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 500)
    private String cuerpo;

    @Column(length = 500)
    private String link;

    @Column(nullable = false)
    private Boolean leida = false;
}