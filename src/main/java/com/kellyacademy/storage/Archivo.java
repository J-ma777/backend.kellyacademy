package com.kellyacademy.storage;

import com.kellyacademy.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity @Table(name="archivos") @Getter @Setter @NoArgsConstructor
public class Archivo extends BaseEntity {
    @Column(nullable=false) private UUID cursoId;
    @Column(nullable=false) private UUID usuarioId;
    @Column(nullable=false,length=20) private String destino;
    @Column(nullable=false,length=20) private String proveedor = "CLOUDINARY";
    @Column(nullable=false,length=255) private String nombre;
    @Column(nullable=false,length=120) private String mime;
    @Column(nullable=false) private long bytes;
    @Column(nullable=false,length=255,unique=true) private String publicId;
    @Column(nullable=false,length=10) private String resourceType;
    @Column(nullable=false,length=500) private String url;
}
