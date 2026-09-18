package com.kellyacademy.course.mapper;

import com.kellyacademy.course.dto.request.ActualizarClaseRequest;
import com.kellyacademy.course.dto.request.CrearClaseRequest;
import com.kellyacademy.course.dto.response.ClaseResponse;
import com.kellyacademy.course.dto.response.ClaseResumenResponse;
import com.kellyacademy.course.entity.Clase;
import com.kellyacademy.course.entity.Semana;
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
public interface ClaseMapper {

    @Mapping(target = "semanaId", source = "semana", qualifiedByName = "extraerIdSemana")
    ClaseResponse toResponse(Clase clase);

    ClaseResumenResponse toResumenResponse(Clase clase);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "semana", ignore = true)
    Clase toEntity(CrearClaseRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "semana", ignore = true)
    void actualizarDesdeRequest(ActualizarClaseRequest request, @MappingTarget Clase clase);

    @Named("extraerIdSemana")
    default UUID extraerIdSemana(Semana semana) {
        return semana == null ? null : semana.getId();
    }
}