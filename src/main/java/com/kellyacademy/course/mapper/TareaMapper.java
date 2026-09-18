package com.kellyacademy.course.mapper;

import com.kellyacademy.course.dto.request.ActualizarTareaRequest;
import com.kellyacademy.course.dto.request.CrearTareaRequest;
import com.kellyacademy.course.dto.response.TareaResponse;
import com.kellyacademy.course.dto.response.TareaResumenResponse;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.entity.Tarea;
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
public interface TareaMapper {

    @Mapping(target = "semanaId", source = "semana", qualifiedByName = "extraerIdSemana")
    TareaResponse toResponse(Tarea tarea);

    TareaResumenResponse toResumenResponse(Tarea tarea);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "semana", ignore = true)
    Tarea toEntity(CrearTareaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "semana", ignore = true)
    void actualizarDesdeRequest(ActualizarTareaRequest request, @MappingTarget Tarea tarea);

    @Named("extraerIdSemana")
    default UUID extraerIdSemana(Semana semana) {
        return semana == null ? null : semana.getId();
    }
}