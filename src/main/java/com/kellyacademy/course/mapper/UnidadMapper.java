package com.kellyacademy.course.mapper;

import com.kellyacademy.course.dto.request.ActualizarUnidadRequest;
import com.kellyacademy.course.dto.request.CrearUnidadRequest;
import com.kellyacademy.course.dto.response.UnidadResponse;
import com.kellyacademy.course.dto.response.UnidadResumenResponse;
import com.kellyacademy.course.entity.Curso;
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
public interface UnidadMapper {

    @Mapping(target = "cursoId", source = "curso", qualifiedByName = "extraerIdCurso")
    UnidadResponse toResponse(Unidad unidad);

    UnidadResumenResponse toResumenResponse(Unidad unidad);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "curso", ignore = true)
    Unidad toEntity(CrearUnidadRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "curso", ignore = true)
    @Mapping(target = "numero", ignore = true)
    void actualizarDesdeRequest(ActualizarUnidadRequest request, @MappingTarget Unidad unidad);

    @Named("extraerIdCurso")
    default UUID extraerIdCurso(Curso curso) {
        return curso == null ? null : curso.getId();
    }
}