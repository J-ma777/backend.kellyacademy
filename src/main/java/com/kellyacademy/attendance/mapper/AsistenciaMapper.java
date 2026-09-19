package com.kellyacademy.attendance.mapper;

import com.kellyacademy.attendance.dto.request.ActualizarAsistenciaRequest;
import com.kellyacademy.attendance.dto.request.CrearAsistenciaRequest;
import com.kellyacademy.attendance.dto.response.AsistenciaResponse;
import com.kellyacademy.attendance.dto.response.AsistenciaResumenResponse;
import com.kellyacademy.attendance.entity.Asistencia;
import com.kellyacademy.course.entity.Clase;
import com.kellyacademy.user.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface AsistenciaMapper {

    @Mapping(target = "claseId", source = "clase", qualifiedByName = "extraerIdClase")
    @Mapping(target = "claseTitulo", source = "clase", qualifiedByName = "extraerTituloClase")
    @Mapping(target = "claseFechaHora", source = "clase", qualifiedByName = "extraerFechaHoraClase")
    @Mapping(target = "estudianteId", source = "estudiante", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "estudianteNombreCompleto", source = "estudiante", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "estudianteCorreo", source = "estudiante", qualifiedByName = "extraerCorreo")
    AsistenciaResponse toResponse(Asistencia entity);

    @Mapping(target = "claseId", source = "clase", qualifiedByName = "extraerIdClase")
    @Mapping(target = "claseTitulo", source = "clase", qualifiedByName = "extraerTituloClase")
    @Mapping(target = "claseFechaHora", source = "clase", qualifiedByName = "extraerFechaHoraClase")
    @Mapping(target = "estudianteId", source = "estudiante", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "estudianteNombreCompleto", source = "estudiante", qualifiedByName = "extraerNombreCompleto")
    AsistenciaResumenResponse toResumenResponse(Asistencia entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "clase", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "registradoAt", ignore = true)
    Asistencia toEntity(CrearAsistenciaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "clase", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "registradoAt", ignore = true)
    void actualizarDesdeRequest(ActualizarAsistenciaRequest request, @MappingTarget Asistencia entity);

    // Helpers: extraen campos planos sin disparar SELECTs adicionales

    @Named("extraerIdClase")
    default UUID extraerIdClase(Clase clase) {
        return clase == null ? null : clase.getId();
    }

    @Named("extraerTituloClase")
    default String extraerTituloClase(Clase clase) {
        return clase == null ? null : clase.getTitulo();
    }

    @Named("extraerFechaHoraClase")
    default LocalDateTime extraerFechaHoraClase(Clase clase) {
        return clase == null ? null : clase.getFechaHora();
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