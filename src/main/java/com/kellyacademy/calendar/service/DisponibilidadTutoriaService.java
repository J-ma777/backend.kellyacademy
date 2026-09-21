package com.kellyacademy.calendar.service;

import com.kellyacademy.calendar.dto.request.ActualizarDisponibilidadRequest;
import com.kellyacademy.calendar.dto.request.CrearDisponibilidadRequest;
import com.kellyacademy.calendar.dto.response.DisponibilidadResponse;
import com.kellyacademy.calendar.dto.response.DisponibilidadResumenResponse;
import com.kellyacademy.calendar.entity.DisponibilidadTutoria;
import com.kellyacademy.calendar.mapper.DisponibilidadMapper;
import com.kellyacademy.calendar.repository.DisponibilidadTutoriaRepository;
import com.kellyacademy.calendar.specification.DisponibilidadTutoriaSpecifications;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.shared.util.SecurityUtils;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DisponibilidadTutoriaService {

    private final DisponibilidadTutoriaRepository disponibilidadRepository;
    private final DisponibilidadMapper disponibilidadMapper;
    private final UsuarioRepository usuarioRepository;

    // -------- listar --------

    @Transactional(readOnly = true)
    public Page<DisponibilidadResumenResponse> listar(
            UUID docenteId,
            DayOfWeek diaSemana,
            Boolean bloqueada,
            Pageable pageable
    ) {
        Specification<DisponibilidadTutoria> spec = Specification.allOf(
                DisponibilidadTutoriaSpecifications.porDocente(docenteId),
                DisponibilidadTutoriaSpecifications.porDiaSemana(diaSemana),
                DisponibilidadTutoriaSpecifications.porBloqueada(bloqueada)
        );

        return disponibilidadRepository.findAll(spec, pageable)
                .map(disponibilidadMapper::toResumenResponse);
    }

    // -------- obtener --------

    @Transactional(readOnly = true)
    public DisponibilidadResponse obtener(UUID id) {
        DisponibilidadTutoria entity = disponibilidadRepository.findWithDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DisponibilidadTutoria", "id", id));

        return disponibilidadMapper.toResponse(entity);
    }

    // -------- crear --------

    public DisponibilidadResponse crear(CrearDisponibilidadRequest request) {
        // Deuda #41: docente dueno del recurso o ADMIN.
        SecurityUtils.validarDocenteDuenoOAdmin(
                request.docenteId(),
                "No tiene permiso para crear disponibilidad de otro docente."
        );

        // Deuda #39: rango horario valido.
        validarRangoHorario(request.horaInicio(), request.horaFin());

        // Deuda #40: no solape. Se valida ANTES de cargar al docente:
        // si hay solape, no tiene sentido ir a la DB a buscar el Usuario.
        validarNoSolape(request.docenteId(), request.diaSemana(),
                request.horaInicio(), request.horaFin(), null);

        Usuario docente = usuarioRepository.findById(request.docenteId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.docenteId()));

        DisponibilidadTutoria entity = disponibilidadMapper.toEntity(request);
        entity.setDocente(docente);
        entity.setBloqueada(false);

        DisponibilidadTutoria guardada = disponibilidadRepository.save(entity);
        return disponibilidadMapper.toResponse(guardada);
    }

    // -------- actualizar --------

    public DisponibilidadResponse actualizar(UUID id, ActualizarDisponibilidadRequest request) {
        DisponibilidadTutoria entity = disponibilidadRepository.findWithDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DisponibilidadTutoria", "id", id));

        // Deuda #41: solo el docente dueno del bloque o ADMIN.
        validarDocenteDuenoDelRecurso(entity);

        validarRangoHorario(request.horaInicio(), request.horaFin());

        // Deuda #40: no solape, excluyendo el propio bloque.
        validarNoSolape(entity.getDocente().getId(), request.diaSemana(),
                request.horaInicio(), request.horaFin(), entity.getId());

        disponibilidadMapper.actualizarDesdeRequest(request, entity);

        DisponibilidadTutoria guardada = disponibilidadRepository.save(entity);
        return disponibilidadMapper.toResponse(guardada);
    }

    // -------- eliminar --------

    public void eliminar(UUID id) {
        DisponibilidadTutoria entity = disponibilidadRepository.findWithDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DisponibilidadTutoria", "id", id));

        disponibilidadRepository.delete(entity);
    }

    // -------- validaciones de negocio --------

    // Deuda #39: horaFin > horaInicio.
    private void validarRangoHorario(LocalTime horaInicio, LocalTime horaFin) {
        if (!horaFin.isAfter(horaInicio)) {
            throw new BusinessException(
                    "HORAS_INVALIDAS",
                    "La hora de fin debe ser posterior a la hora de inicio."
            );
        }
    }

    // Deuda #40: no solape de bloques del mismo docente y dia.
    private void validarNoSolape(UUID docenteId, DayOfWeek diaSemana,
                                 LocalTime horaInicio, LocalTime horaFin, UUID excluirId) {
        List<DisponibilidadTutoria> solapadas = disponibilidadRepository
                .findSolapadas(docenteId, diaSemana, horaInicio, horaFin, excluirId);

        if (!solapadas.isEmpty()) {
            throw new BusinessException(
                    "BLOQUE_SOLAPADO",
                    "El bloque se solapa con otro existente del mismo docente y dia."
            );
        }
    }

    // Deuda #41: valida que el autenticado sea el docente dueno del bloque o ADMIN.
    private void validarDocenteDuenoDelRecurso(DisponibilidadTutoria entity) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        if (!entity.getDocente().getId().equals(SecurityUtils.getUsuarioAutenticadoId())) {
            throw new AccessDeniedException("No tiene permiso sobre este bloque de disponibilidad.");
        }
    }
}