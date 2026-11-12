package com.kellyacademy.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ArchivoBibliotecaRepository extends JpaRepository<ArchivoBiblioteca, UUID> {
    Optional<ArchivoBiblioteca> findByUrl(String url);
}
