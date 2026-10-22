package com.kellyacademy.storage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ArchivoRepository extends JpaRepository<Archivo,UUID> { java.util.Optional<Archivo> findByUrl(String url); }
