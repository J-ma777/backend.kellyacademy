package com.kellyacademy.course.mapper;

import com.kellyacademy.course.dto.request.ActualizarMaterialRequest;
import com.kellyacademy.course.dto.request.CrearMaterialRequest;
import com.kellyacademy.course.dto.response.MaterialResponse;
import com.kellyacademy.course.dto.response.MaterialResumenResponse;
import com.kellyacademy.course.entity.Material;
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
public interface MaterialMapper {

    @Mapping(target = "semanaId", source = "semana", qualifiedByName = "extraerIdSemana")
    MaterialResponse toResponse(Material material);

    MaterialResumenResponse toResumenResponse(Material material);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "semana", ignore = true)
    Material toEntity(CrearMaterialRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "semana", ignore = true)
    void actualizarDesdeRequest(ActualizarMaterialRequest request, @MappingTarget Material material);

    @Named("extraerIdSemana")
    default UUID extraerIdSemana(Semana semana) {
        return semana == null ? null : semana.getId();
    }
}