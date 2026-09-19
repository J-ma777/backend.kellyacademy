package com.kellyacademy.enrollment.mapper;

import com.kellyacademy.course.entity.Tarea;
import com.kellyacademy.enrollment.dto.request.ActualizarEntregaRequest;
import com.kellyacademy.enrollment.dto.request.CrearEntregaRequest;
import com.kellyacademy.enrollment.dto.response.EntregaResponse;
import com.kellyacademy.enrollment.dto.response.EntregaResumenResponse;
import com.kellyacademy.enrollment.entity.Entrega;
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
public interface EntregaMapper {

    @Mapping(target = "tareaId", source = "tarea", qualifiedByName = "extraerIdTarea")
    @Mapping(target = "tareaTitulo", source = "tarea", qualifiedByName = "extraerTituloTarea")
    @Mapping(target = "estudianteId", source = "estudiante", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "estudianteNombreCompleto", source = "estudiante", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "estudianteCorreo", source = "estudiante", qualifiedByName = "extraerCorreo")
    EntregaResponse toResponse(Entrega entity);

    @Mapping(target = "tareaId", source = "tarea", qualifiedByName = "extraerIdTarea")
    @Mapping(target = "tareaTitulo", source = "tarea", qualifiedByName = "extraerTituloTarea")
    @Mapping(target = "estudianteId", source = "estudiante", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "estudianteNombreCompleto", source = "estudiante", qualifiedByName = "extraerNombreCompleto")
    EntregaResumenResponse toResumenResponse(Entrega entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "tarea", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "enviadoAt", ignore = true)
    @Mapping(target = "nota", ignore = true)
    @Mapping(target = "retroalimentacion", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Entrega toEntity(CrearEntregaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "tarea", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "enviadoAt", ignore = true)
    @Mapping(target = "nota", ignore = true)
    @Mapping(target = "retroalimentacion", ignore = true)
    @Mapping(target = "estado", ignore = true)
    void actualizarDesdeRequest(ActualizarEntregaRequest request, @MappingTarget Entrega entity);

    // Helpers

    @Named("extraerIdTarea")
    default UUID extraerIdTarea(Tarea tarea) {
        return tarea == null ? null : tarea.getId();
    }

    @Named("extraerTituloTarea")
    default String extraerTituloTarea(Tarea tarea) {
        return tarea == null ? null : tarea.getTitulo();
    }

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
}