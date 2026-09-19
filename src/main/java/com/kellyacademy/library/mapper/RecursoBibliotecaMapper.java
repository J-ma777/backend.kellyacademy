package com.kellyacademy.library.mapper;

import com.kellyacademy.library.dto.request.ActualizarRecursoRequest;
import com.kellyacademy.library.dto.request.CrearRecursoRequest;
import com.kellyacademy.library.dto.response.RecursoResponse;
import com.kellyacademy.library.dto.response.RecursoResumenResponse;
import com.kellyacademy.library.entity.RecursoBiblioteca;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface RecursoBibliotecaMapper {

    RecursoResponse toResponse(RecursoBiblioteca entity);

    RecursoResumenResponse toResumenResponse(RecursoBiblioteca entity);

    // El servicio setea: contadorDescargas.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "contadorDescargas", ignore = true)
    RecursoBiblioteca toEntity(CrearRecursoRequest request);

    // Inmutable: contadorDescargas.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "contadorDescargas", ignore = true)
    void actualizarDesdeRequest(ActualizarRecursoRequest request, @MappingTarget RecursoBiblioteca entity);
}