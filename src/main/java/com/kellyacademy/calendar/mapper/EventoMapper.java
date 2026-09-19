package com.kellyacademy.calendar.mapper;

import com.kellyacademy.calendar.dto.request.ActualizarEventoRequest;
import com.kellyacademy.calendar.dto.request.CrearEventoRequest;
import com.kellyacademy.calendar.dto.response.EventoResponse;
import com.kellyacademy.calendar.dto.response.EventoResumenResponse;
import com.kellyacademy.calendar.entity.Evento;
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
public interface EventoMapper {

    @Mapping(target = "usuarioId", source = "usuario", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "usuarioNombreCompleto", source = "usuario", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "cursoId", source = "curso", qualifiedByName = "extraerIdCurso")
    @Mapping(target = "cursoTitulo", source = "curso", qualifiedByName = "extraerTituloCurso")
    EventoResponse toResponse(Evento entity);

    @Mapping(target = "usuarioId", source = "usuario", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "usuarioNombreCompleto", source = "usuario", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "cursoId", source = "curso", qualifiedByName = "extraerIdCurso")
    @Mapping(target = "cursoTitulo", source = "curso", qualifiedByName = "extraerTituloCurso")
    EventoResumenResponse toResumenResponse(Evento entity);

    // El servicio resuelve y setea: usuario, curso.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "curso", ignore = true)
    Evento toEntity(CrearEventoRequest request);

    // Inmutables: id, fechas, usuario, curso.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "curso", ignore = true)
    void actualizarDesdeRequest(ActualizarEventoRequest request, @MappingTarget Evento entity);

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

    @Named("extraerIdCurso")
    default UUID extraerIdCurso(Curso curso) {
        return curso == null ? null : curso.getId();
    }

    @Named("extraerTituloCurso")
    default String extraerTituloCurso(Curso curso) {
        return curso == null ? null : curso.getTitulo();
    }
}