package com.kellyacademy.storage;

import com.kellyacademy.shared.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "archivos_biblioteca")
@Getter @Setter @NoArgsConstructor
public class ArchivoBiblioteca extends BaseEntity {
    private UUID usuarioId;
    @Column(nullable = false, length = 20) private String proveedor;
    @Column(nullable = false, length = 255) private String nombre;
    @Column(nullable = false, length = 120) private String mime;
    @Column(nullable = false) private long bytes;
    @Column(nullable = false, length = 255, unique = true) private String publicId;
    @Column(nullable = false, length = 500, unique = true) private String url;
}
