package com.kellyacademy.library.service;

import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.enums.TipoMaterial;
import com.kellyacademy.library.dto.request.ActualizarRecursoRequest;
import com.kellyacademy.library.dto.request.CrearRecursoRequest;
import com.kellyacademy.library.dto.response.RecursoResponse;
import com.kellyacademy.library.dto.response.RecursoResumenResponse;
import com.kellyacademy.library.entity.RecursoBiblioteca;
import com.kellyacademy.library.mapper.RecursoBibliotecaMapper;
import com.kellyacademy.library.repository.RecursoBibliotecaRepository;
import com.kellyacademy.library.specification.RecursoBibliotecaSpecifications;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.shared.util.UrlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RecursoBibliotecaService {

    private final RecursoBibliotecaRepository recursoRepository;
    private final RecursoBibliotecaMapper recursoMapper;

    // -------- listar --------

    @Transactional(readOnly = true)
    public Page<RecursoResumenResponse> listar(
            String categoria,
            NivelCefr nivelCefr,
            TipoMaterial tipo,
            String titulo,
            Pageable pageable
    ) {
        Specification<RecursoBiblioteca> spec = Specification.allOf(
                RecursoBibliotecaSpecifications.porCategoria(categoria),
                RecursoBibliotecaSpecifications.porNivelCefr(nivelCefr),
                RecursoBibliotecaSpecifications.porTipo(tipo),
                RecursoBibliotecaSpecifications.tituloContiene(titulo)
        );

        return recursoRepository.findAll(spec, pageable)
                .map(recursoMapper::toResumenResponse);
    }

    // -------- obtener --------

    @Transactional(readOnly = true)
    public RecursoResponse obtener(UUID id) {
        RecursoBiblioteca recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RecursoBiblioteca", "id", id));

        return recursoMapper.toResponse(recurso);
    }

    // -------- crear --------

    public RecursoResponse crear(CrearRecursoRequest request) {
        validarUrlArchivo(request.urlArchivo());
        validarUrlExterno(request.urlExterno());
        validarAlMenosUnaUrl(request.urlArchivo(), request.urlExterno());

        RecursoBiblioteca entity = recursoMapper.toEntity(request);
        entity.setContadorDescargas(0);

        RecursoBiblioteca guardada = recursoRepository.save(entity);
        return recursoMapper.toResponse(guardada);
    }

    // -------- actualizar --------

    public RecursoResponse actualizar(UUID id, ActualizarRecursoRequest request) {
        RecursoBiblioteca recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RecursoBiblioteca", "id", id));

        validarUrlArchivo(request.urlArchivo());
        validarUrlExterno(request.urlExterno());
        validarAlMenosUnaUrl(request.urlArchivo(), request.urlExterno());

        // El mapper ignora contadorDescargas: inmutable via PUT.
        recursoMapper.actualizarDesdeRequest(request, recurso);

        RecursoBiblioteca guardada = recursoRepository.save(recurso);
        return recursoMapper.toResponse(guardada);
    }

    // -------- eliminar --------

    public void eliminar(UUID id) {
        RecursoBiblioteca recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RecursoBiblioteca", "id", id));
        recursoRepository.delete(recurso);
    }

    // -------- validaciones --------

    // Deuda #51: formato de URL con el helper compartido.
    private void validarUrlArchivo(String url) {
        if (url != null && !url.isBlank() && !UrlValidator.esFormatoValido(url)) {
            throw new BusinessException(
                    "URL_INVALIDA",
                    "La URL del archivo no tiene un formato valido: " + url
            );
        }
    }

    private void validarUrlExterno(String url) {
        if (url != null && !url.isBlank() && !UrlValidator.esFormatoValido(url)) {
            throw new BusinessException(
                    "URL_INVALIDA",
                    "La URL externa no tiene un formato valido: " + url
            );
        }
    }

    // Deuda #49: al menos una URL presente y no blank.
    private void validarAlMenosUnaUrl(String urlArchivo, String urlExterno) {
        boolean archivoPresente = urlArchivo != null && !urlArchivo.isBlank();
        boolean externoPresente = urlExterno != null && !urlExterno.isBlank();

        if (!archivoPresente && !externoPresente) {
            throw new BusinessException(
                    "RECURSO_SIN_URL",
                    "El recurso debe tener al menos una URL (archivo o externa)."
            );
        }
    }
}