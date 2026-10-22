package com.kellyacademy.enrollment.service;

import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.course.service.AccesoCursoService;
import com.kellyacademy.enrollment.dto.request.EnviarEntregaRequest;
import com.kellyacademy.enrollment.dto.response.EntregaResponse;
import com.kellyacademy.enrollment.entity.Entrega;
import com.kellyacademy.enrollment.enums.EstadoEntrega;
import com.kellyacademy.enrollment.mapper.EntregaMapper;
import com.kellyacademy.enrollment.repository.EntregaRepository;
import com.kellyacademy.shared.config.AppTime;
import com.kellyacademy.shared.exception.*;
import com.kellyacademy.shared.util.SecurityUtils;
import com.kellyacademy.shared.util.UrlValidator;
import com.kellyacademy.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class EnvioEstudianteService {
    private final TareaRepository tareaRepository;
    private final EntregaRepository entregaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntregaMapper mapper;
    private final AccesoCursoService acceso;
    private final com.kellyacademy.storage.ArchivoAccesoService archivos;
    public EntregaResponse enviar(EnviarEntregaRequest r) {
        var tarea = tareaRepository.findWithSemanaCursoDocenteById(r.tareaId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarea", "id", r.tareaId()));
        acceso.validarContenido(tarea);
        acceso.validarEstudianteActivo(tarea.getSemana().getUnidad().getCurso());
        String archivo = r.urlArchivo() == null || r.urlArchivo().isBlank() ? null : r.urlArchivo().trim();
        String texto = r.textoRespuesta() == null || r.textoRespuesta().isBlank() ? null : r.textoRespuesta().trim();
        if (archivo == null && texto == null) throw new BusinessException("ENTREGA_VACIA", "Añade un archivo enlazado o una respuesta de texto");
        if (!UrlValidator.esFormatoValido(archivo)) throw new BusinessException("URL_INVALIDA", "El enlace del archivo no es valido");
        archivos.validarVinculo(archivo,tarea.getSemana().getUnidad().getCurso().getId(),com.kellyacademy.storage.ArchivoService.Destino.ENTREGA);
        var estudianteId = SecurityUtils.getUsuarioAutenticadoId();
        Entrega entrega = entregaRepository.findByTareaIdAndEstudianteId(tarea.getId(), estudianteId).orElseGet(Entrega::new);
        if (entrega.getEstado() == EstadoEntrega.CALIFICADA) throw new BusinessException("ENTREGA_CALIFICADA", "La entrega ya fue calificada y no se puede reemplazar");
        entrega.setTarea(tarea);
        entrega.setEstudiante(usuarioRepository.getReferenceById(estudianteId));
        entrega.setUrlArchivo(archivo);
        entrega.setTextoRespuesta(texto);
        var ahora = LocalDateTime.now(AppTime.ZONA_NEGOCIO);
        entrega.setEnviadoAt(ahora);
        entrega.setEstado(tarea.getFechaLimite() != null && ahora.isAfter(tarea.getFechaLimite()) ? EstadoEntrega.TARDE : EstadoEntrega.PENDIENTE);
        return mapper.toResponse(entregaRepository.save(entrega));
    }
}
