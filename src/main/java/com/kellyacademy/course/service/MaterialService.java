package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.ActualizarMaterialRequest;
import com.kellyacademy.course.dto.request.CrearMaterialRequest;
import com.kellyacademy.course.dto.response.MaterialResponse;
import com.kellyacademy.course.dto.response.MaterialResumenResponse;
import com.kellyacademy.course.entity.Material;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.mapper.MaterialMapper;
import com.kellyacademy.course.repository.MaterialRepository;
import com.kellyacademy.course.repository.SemanaRepository;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.shared.util.SecurityUtils;
import com.kellyacademy.shared.util.UrlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final SemanaRepository semanaRepository;
    private final MaterialMapper materialMapper;

    @Transactional(readOnly = true)
    public List<MaterialResumenResponse> listar(UUID semanaId) {
        if (semanaId == null) {
            throw new BusinessException(
                    "SEMANA_ID_REQUERIDO",
                    "El filtro semanaId es obligatorio para listar materiales"
            );
        }
        if (!semanaRepository.existsById(semanaId)) {
            throw new ResourceNotFoundException("Semana", "id", semanaId);
        }
        return materialRepository.findBySemanaId(semanaId).stream()
                .map(materialMapper::toResumenResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaterialResponse obtener(UUID id) {
        Material material = materialRepository.findWithSemanaCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material", "id", id));
        return materialMapper.toResponse(material);
    }

    public MaterialResponse crear(CrearMaterialRequest request) {

        Semana semana = semanaRepository.findWithUnidadCursoDocenteById(request.semanaId())
                .orElseThrow(() -> new ResourceNotFoundException("Semana", "id", request.semanaId()));

        validarAutorizacion(semana);
        validarUrls(request.urlArchivo(), request.urlExterno(), true);

        Material material = materialMapper.toEntity(request);
        material.setSemana(semana);

        Material guardado = materialRepository.save(material);

        return materialMapper.toResponse(
                materialRepository.findWithSemanaCursoDocenteById(guardado.getId()).orElseThrow()
        );
    }

    public MaterialResponse actualizar(UUID id, ActualizarMaterialRequest request) {

        Material material = materialRepository.findWithSemanaCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material", "id", id));

        validarAutorizacion(material.getSemana());
        validarUrls(request.urlArchivo(), request.urlExterno(), true);

        materialMapper.actualizarDesdeRequest(request, material);

        return materialMapper.toResponse(material);
    }

    public void eliminar(UUID id) {

        Material material = materialRepository.findWithSemanaCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material", "id", id));

        validarAutorizacion(material.getSemana());

        materialRepository.delete(material);
    }

    private void validarAutorizacion(Semana semana) {
        UUID docenteId = semana.getUnidad().getCurso().getDocente().getId();
        SecurityUtils.validarDocenteDuenoOAdmin(
                docenteId,
                "No tienes permisos para modificar este material"
        );
    }

    // Regla de negocio #17: al menos una URL debe estar presente y ser valida.
    // Si la URL viene con contenido, se valida formato.
    private void validarUrls(String urlArchivo, String urlExterno, boolean alMenosUna) {

        boolean archivoVacio = urlArchivo == null || urlArchivo.isBlank();
        boolean externoVacio = urlExterno == null || urlExterno.isBlank();

        if (alMenosUna && archivoVacio && externoVacio) {
            throw new BusinessException(
                    "MATERIAL_SIN_URL",
                    "El material debe tener al menos urlArchivo o urlExterno"
            );
        }

        if (!UrlValidator.esFormatoValido(urlArchivo)) {
            throw new BusinessException(
                    "URL_INVALIDA",
                    "La URL del archivo no tiene un formato valido: " + urlArchivo
            );
        }

        if (!UrlValidator.esFormatoValido(urlExterno)) {
            throw new BusinessException(
                    "URL_INVALIDA",
                    "La URL externa no tiene un formato valido: " + urlExterno
            );
        }
    }
}