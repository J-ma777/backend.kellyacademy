package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.ActualizarClaseRequest;
import com.kellyacademy.course.dto.request.CrearClaseRequest;
import com.kellyacademy.course.dto.response.ClaseResponse;
import com.kellyacademy.course.dto.response.ClaseResumenResponse;
import com.kellyacademy.course.entity.Clase;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.mapper.ClaseMapper;
import com.kellyacademy.course.repository.ClaseRepository;
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
public class ClaseService {

    private final ClaseRepository claseRepository;
    private final SemanaRepository semanaRepository;
    private final ClaseMapper claseMapper;
    private static final String RECURSO = "Clase";
    private static final String RECURSO_SEMANA = "Semana";

    @Transactional(readOnly = true)
    public List<ClaseResumenResponse> listar(UUID semanaId) {
        if (semanaId == null) {
            throw new BusinessException(
                    "SEMANA_ID_REQUERIDO",
                    "El filtro semanaId es obligatorio para listar clases"
            );
        }
        if (!semanaRepository.existsById(semanaId)) {
            throw new ResourceNotFoundException(RECURSO_SEMANA, "id", semanaId);
        }
        return claseRepository.findBySemanaIdOrderByFechaHoraAsc(semanaId).stream()
                .map(claseMapper::toResumenResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClaseResponse obtener(UUID id) {
        Clase clase = claseRepository.findWithSemanaCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));
        return claseMapper.toResponse(clase);
    }

    public ClaseResponse crear(CrearClaseRequest request) {

        Semana semana = semanaRepository.findWithUnidadCursoDocenteById(request.semanaId())
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_SEMANA, "id", request.semanaId()));

        validarAutorizacion(semana);

        validarUrl(request.urlVivo(), "La URL en vivo no tiene un formato valido");
        validarUrl(request.urlGrabacion(), "La URL de grabacion no tiene un formato valido");

        Clase clase = claseMapper.toEntity(request);
        clase.setSemana(semana);

        Clase guardada = claseRepository.save(clase);

        return claseMapper.toResponse(
                claseRepository.findWithSemanaCursoDocenteById(guardada.getId()).orElseThrow()
        );
    }

    public ClaseResponse actualizar(UUID id, ActualizarClaseRequest request) {

        Clase clase = claseRepository.findWithSemanaCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarAutorizacion(clase.getSemana());

        validarUrl(request.urlVivo(), "La URL en vivo no tiene un formato valido");
        validarUrl(request.urlGrabacion(), "La URL de grabacion no tiene un formato valido");

        claseMapper.actualizarDesdeRequest(request, clase);

        return claseMapper.toResponse(clase);
    }

    public void eliminar(UUID id) {

        Clase clase = claseRepository.findWithSemanaCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarAutorizacion(clase.getSemana());

        claseRepository.delete(clase);
    }

    // ADMIN o docente dueno del curso al que pertenece la semana.
    private void validarAutorizacion(Semana semana) {
        UUID docenteId = semana.getUnidad().getCurso().getDocente().getId();
        SecurityUtils.validarDocenteDuenoOAdmin(
                docenteId,
                "No tienes permisos para modificar esta clase"
        );
    }

    private void validarUrl(String url, String mensaje) {
        if (!UrlValidator.esFormatoValido(url)) {
            throw new BusinessException("URL_INVALIDA", mensaje + ": " + url);
        }
    }
}