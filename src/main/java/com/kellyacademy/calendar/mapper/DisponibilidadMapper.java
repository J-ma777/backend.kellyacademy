package com.kellyacademy.calendar.mapper;

import com.kellyacademy.calendar.dto.request.ActualizarDisponibilidadRequest;
import com.kellyacademy.calendar.dto.request.CrearDisponibilidadRequest;
import com.kellyacademy.calendar.dto.response.DisponibilidadResponse;
import com.kellyacademy.calendar.dto.response.DisponibilidadResumenResponse;
import com.kellyacademy.calendar.entity.DisponibilidadTutoria;
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
public interface DisponibilidadMapper {

    @Mapping(target = "docenteId", source = "docente", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "docenteNombreCompleto", source = "docente", qualifiedByName = "extraerNombreCompleto")
    DisponibilidadResponse toResponse(DisponibilidadTutoria entity);

    @Mapping(target = "docenteId", source = "docente", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "docenteNombreCompleto", source = "docente", qualifiedByName = "extraerNombreCompleto")
    DisponibilidadResumenResponse toResumenResponse(DisponibilidadTutoria entity);

    // El servicio resuelve y setea: docente, bloqueada.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "docente", ignore = true)
    @Mapping(target = "bloqueada", ignore = true)
    DisponibilidadTutoria toEntity(CrearDisponibilidadRequest request);

    // Inmutable: docente.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "docente", ignore = true)
    void actualizarDesdeRequest(ActualizarDisponibilidadRequest request, @MappingTarget DisponibilidadTutoria entity);

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
}