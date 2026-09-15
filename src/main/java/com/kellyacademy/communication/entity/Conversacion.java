package com.kellyacademy.communication.entity;

import com.kellyacademy.course.entity.Curso;
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
        name = "conversaciones",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_conversaciones_participantes_curso",
                columnNames = {"curso_id", "participante_1_id", "participante_2_id"}
        )
)
public class Conversacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participante_1_id", nullable = false)
    private Usuario participante1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participante_2_id", nullable = false)
    private Usuario participante2;

    @Column(length = 200)
    private String asunto;

    @Column(name = "ultimo_mensaje_at")
    private LocalDateTime ultimoMensajeAt;
}