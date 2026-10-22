package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.*;
import com.kellyacademy.course.entity.*;
import com.kellyacademy.course.enums.TipoActividad;
import com.kellyacademy.course.repository.*;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.shared.exception.*;
import com.kellyacademy.shared.util.SecurityUtils;
import com.kellyacademy.user.repository.UsuarioRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ContenidoCursoService {
    private final AccesoCursoService acceso;
    private final com.kellyacademy.storage.ArchivoAccesoService archivos;
    private final SemanaRepository semanaRepository;
    private final MaterialRepository materialRepository;
    private final ClaseRepository claseRepository;
    private final TareaRepository tareaRepository;
    private final MaterialService materialService;
    private final ClaseService claseService;
    private final TareaService tareaService;
    private final ProgresoContenidoRepository progresoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final Validator validator;

    public void guardar(UUID cursoId, String tipo, UUID id, GuardarContenidoRequest r) {
        Curso curso = acceso.obtener(cursoId);
        acceso.validarGestor(curso);
        Semana semana = semanaRepository.findWithUnidadCursoDocenteById(r.semanaId())
                .orElseThrow(() -> new ResourceNotFoundException("Semana", "id", r.semanaId()));
        if (!semana.getUnidad().getCurso().getId().equals(cursoId)) throw new BusinessException("SEMANA_AJENA", "La semana no pertenece al curso");
        if (id != null && !obtener(tipo, id).getSemana().getId().equals(r.semanaId()))
            throw new BusinessException("RECURSO_AJENO", "El recurso no pertenece a esta semana");
        if("MATERIAL".equals(tipo))archivos.validarVinculo(r.urlArchivo(),cursoId,com.kellyacademy.storage.ArchivoService.Destino.MATERIAL);
        if("TASK".equals(tipo))archivos.validarVinculo(r.urlExterno(),cursoId,com.kellyacademy.storage.ArchivoService.Destino.ACTIVIDAD);
        UUID guardado;
        switch (tipo) {
            case "MATERIAL" -> {
                if (id == null) guardado = materialService.crear(validar(new CrearMaterialRequest(r.semanaId(), r.titulo(), r.descripcion(), r.urlArchivo(), r.tipo(), r.tamanoMb(), r.urlExterno()))).id();
                else guardado = materialService.actualizar(id, validar(new ActualizarMaterialRequest(r.titulo(), r.descripcion(), r.urlArchivo(), r.tipo(), r.tamanoMb(), r.urlExterno()))).id();
            }
            case "CLASS" -> {
                if (id == null) guardado = claseService.crear(validar(new CrearClaseRequest(r.semanaId(), r.titulo(), r.descripcion(), r.urlExterno(), r.urlGrabacion(), r.fechaHora(), r.duracionMinutos(), r.sala()))).id();
                else guardado = claseService.actualizar(id, validar(new ActualizarClaseRequest(r.titulo(), r.descripcion(), r.urlExterno(), r.urlGrabacion(), r.fechaHora(), r.duracionMinutos(), r.sala()))).id();
            }
            case "TASK" -> {
                if (id == null) guardado = tareaService.crear(validar(new CrearTareaRequest(r.semanaId(), r.titulo(), r.descripcion(), r.urlExterno(), r.fechaLimite(), r.puntajeMaximo()))).id();
                else guardado = tareaService.actualizar(id, validar(new ActualizarTareaRequest(r.titulo(), r.descripcion(), r.urlExterno(), r.fechaLimite(), r.puntajeMaximo()))).id();
            }
            default -> throw new BusinessException("TIPO_INVALIDO", "Tipo de contenido no valido");
        }
        ContenidoEntity contenido = obtener(tipo, guardado);
        contenido.setPublicado(r.publicado());
        contenido.setVisible(r.visible());
        if (contenido instanceof Tarea tarea) tarea.setTipoActividad(r.tipoActividad() == null ? TipoActividad.TAREA : r.tipoActividad());
    }

    public void publicar(UUID cursoId, String tipo, UUID id, Boolean publicado, Boolean visible) {
        ContenidoEntity contenido = obtener(tipo, id);
        if (!contenido.getSemana().getUnidad().getCurso().getId().equals(cursoId)) throw new BusinessException("RECURSO_AJENO", "El recurso no pertenece al curso");
        acceso.validarGestor(contenido.getSemana().getUnidad().getCurso());
        contenido.setPublicado(publicado);
        contenido.setVisible(visible);
    }

    public void completar(UUID cursoId, String tipo, UUID id) {
        ContenidoEntity contenido = obtener(tipo, id);
        if (!contenido.getSemana().getUnidad().getCurso().getId().equals(cursoId)) throw new BusinessException("RECURSO_AJENO", "El recurso no pertenece al curso");
        acceso.validarContenido(contenido);
        acceso.validarEstudianteActivo(contenido.getSemana().getUnidad().getCurso());
        UUID estudianteId = SecurityUtils.getUsuarioAutenticadoId();
        boolean existe = switch (tipo) {
            case "MATERIAL" -> progresoRepository.existsByEstudianteIdAndMaterialId(estudianteId, id);
            case "CLASS" -> progresoRepository.existsByEstudianteIdAndClaseId(estudianteId, id);
            default -> throw new BusinessException("ACTIVIDAD_CON_ENTREGA", "El avance de una actividad depende de su entrega");
        };
        if (existe) return;
        ProgresoContenido progreso = new ProgresoContenido();
        progreso.setEstudiante(usuarioRepository.getReferenceById(estudianteId));
        if (contenido instanceof Material m) progreso.setMaterial(m);
        else progreso.setClase((Clase) contenido);
        progresoRepository.save(progreso);
    }

    public void retirarAlumno(UUID cursoId, UUID estudianteId) {
        Curso curso = acceso.obtener(cursoId);
        acceso.validarGestor(curso);
        var matricula = matriculaRepository.findByCursoIdAndEstudianteId(cursoId, estudianteId)
                .orElseThrow(() -> new ResourceNotFoundException("Matricula", "estudianteId", estudianteId));
        if (matricula.getEstado() == EstadoMatricula.COMPLETADA)
            throw new BusinessException("MATRICULA_COMPLETADA", "Una matricula completada conserva su historial");
        matricula.setEstado(EstadoMatricula.ABANDONADA);
    }

    private ContenidoEntity obtener(String tipo, UUID id) {
        return switch (tipo) {
            case "MATERIAL" -> materialRepository.findWithSemanaCursoDocenteById(id).orElseThrow(() -> new ResourceNotFoundException("Material", "id", id));
            case "CLASS" -> claseRepository.findWithSemanaCursoDocenteById(id).orElseThrow(() -> new ResourceNotFoundException("Clase", "id", id));
            case "TASK" -> tareaRepository.findWithSemanaCursoDocenteById(id).orElseThrow(() -> new ResourceNotFoundException("Tarea", "id", id));
            default -> throw new BusinessException("TIPO_INVALIDO", "Tipo de contenido no valido");
        };
    }
    private <T> T validar(T request) {
        var errores = validator.validate(request);
        if (!errores.isEmpty()) throw new BusinessException("DATOS_INVALIDOS", errores.stream().map(e -> e.getMessage()).collect(Collectors.joining(". ")));
        return request;
    }
}
