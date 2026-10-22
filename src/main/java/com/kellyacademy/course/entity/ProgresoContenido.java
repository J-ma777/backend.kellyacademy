package com.kellyacademy.course.entity;
import com.kellyacademy.shared.base.BaseEntity;
import com.kellyacademy.user.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(name = "progreso_contenido", uniqueConstraints = {
        @UniqueConstraint(name = "uk_progreso_material", columnNames = {"estudiante_id", "material_id"}),
        @UniqueConstraint(name = "uk_progreso_clase", columnNames = {"estudiante_id", "clase_id"})
})
public class ProgresoContenido extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Usuario estudiante;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Material material;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clase_id")
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Clase clase;
}
