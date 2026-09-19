package com.kellyacademy.calendar.mapper;

import com.kellyacademy.calendar.dto.request.ActualizarTutoriaRequest;
import com.kellyacademy.calendar.dto.request.CrearTutoriaRequest;
import com.kellyacademy.calendar.dto.response.TutoriaResponse;
import com.kellyacademy.calendar.dto.response.TutoriaResumenResponse;
import com.kellyacademy.calendar.entity.Tutoria;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.user.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface TutoriaMapper {

    @Mapping(target = "estudianteId", source = "estudiante", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "estudianteNombreCompleto", source = "estudiante", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "estudianteCorreo", source = "estudiante", qualifiedByName = "extraerCorreo")
    @Mapping(target = "docenteId", source = "docente", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "docenteNombreCompleto", source = "docente", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "cursoId", source = "curso", qualifiedByName = "extraerIdCurso")
    @Mapping(target = "cursoTitulo", source = "curso", qualifiedByName = "extraerTituloCurso")
    TutoriaResponse toResponse(Tutoria entity);

    @Mapping(target = "estudianteId", source = "estudiante", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "estudianteNombreCompleto", source = "estudiante", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "docenteId", source = "docente", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "docenteNombreCompleto", source = "docente", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "cursoId", source = "curso", qualifiedByName = "extraerIdCurso")
    @Mapping(target = "cursoTitulo", source = "curso", qualifiedByName = "extraerTituloCurso")
    TutoriaResumenResponse toResumenResponse(Tutoria entity);

    // El servicio resuelve y setea: estudiante, docente, curso, estado, notas.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "docente", ignore = true)
    @Mapping(target = "curso", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "notas", ignore = true)
    Tutoria toEntity(CrearTutoriaRequest request);

    // Inmutables: estudiante, docente, curso, estado.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "docente", ignore = true)
    @Mapping(target = "curso", ignore = true)
    @Mapping(target = "estado", ignore = true)
    void actualizarDesdeRequest(ActualizarTutoriaRequest request, @MappingTarget Tutoria entity);

    // --- Helpers ---

    @Named("extraerIdUsuario")
    default UUID extraerIdUsuario(Usuario usuario) {
        return usuario == null ? null : usuario.getId();
    }

    @Named("extraerNombreCompleto")
    default String extraerNombreCompleto(Usuario usuario) {
        if (usuario == null) return null;
        return usuario.getNombre() + " " + usuario.getApellido();
    }

    @Named("extraerCorreo")
    default String extraerCorreo(Usuario usuario) {
        return usuario == null ? null : usuario.getCorreoElectronico();
    }

    @Named("extraerIdCurso")
    default UUID extraerIdCurso(Curso curso) {
        return curso == null ? null : curso.getId();
    }

    @Named("extraerTituloCurso")
    default String extraerTituloCurso(Curso curso) {
        return curso == null ? null : curso.getTitulo();
    }
}