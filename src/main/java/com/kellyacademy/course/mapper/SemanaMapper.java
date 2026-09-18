package com.kellyacademy.course.mapper;

import com.kellyacademy.course.dto.request.ActualizarSemanaRequest;
import com.kellyacademy.course.dto.request.CrearSemanaRequest;
import com.kellyacademy.course.dto.response.SemanaResponse;
import com.kellyacademy.course.dto.response.SemanaResumenResponse;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.entity.Unidad;
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
public interface SemanaMapper {

    @Mapping(target = "unidadId", source = "unidad", qualifiedByName = "extraerIdUnidad")
    SemanaResponse toResponse(Semana semana);

    SemanaResumenResponse toResumenResponse(Semana semana);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "unidad", ignore = true)
    @Mapping(target = "esActual", ignore = true)
    Semana toEntity(CrearSemanaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "unidad", ignore = true)
    @Mapping(target = "numero", ignore = true)
    @Mapping(target = "esActual", ignore = true)
    void actualizarDesdeRequest(ActualizarSemanaRequest request, @MappingTarget Semana semana);

    @Named("extraerIdUnidad")
    default UUID extraerIdUnidad(Unidad unidad) {
        return unidad == null ? null : unidad.getId();
    }
}