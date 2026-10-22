package com.kellyacademy.course.entity;
import com.kellyacademy.shared.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@MappedSuperclass
public abstract class ContenidoEntity extends BaseEntity {
    @Column(nullable = false)
    private Boolean publicado = true;
    @Column(nullable = false)
    private Boolean visible = true;
    public abstract Semana getSemana();
}
