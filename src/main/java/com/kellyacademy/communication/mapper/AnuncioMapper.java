package com.kellyacademy.communication.mapper;

import com.kellyacademy.communication.dto.request.ActualizarAnuncioRequest;
import com.kellyacademy.communication.dto.request.CrearAnuncioRequest;
import com.kellyacademy.communication.dto.response.AnuncioResponse;
import com.kellyacademy.communication.dto.response.AnuncioResumenResponse;
import com.kellyacademy.communication.entity.Anuncio;
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
public interface AnuncioMapper {

    @Mapping(target = "cursoId", source = "curso", qualifiedByName = "extraerIdCurso")
    @Mapping(target = "cursoTitulo", source = "curso", qualifiedByName = "extraerTituloCurso")
    @Mapping(target = "autorId", source = "autor", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "autorNombreCompleto", source = "autor", qualifiedByName = "extraerNombreCompleto")
    AnuncioResponse toResponse(Anuncio entity);

    @Mapping(target = "cursoId", source = "curso", qualifiedByName = "extraerIdCurso")
    @Mapping(target = "cursoTitulo", source = "curso", qualifiedByName = "extraerTituloCurso")
    @Mapping(target = "autorId", source = "autor", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "autorNombreCompleto", source = "autor", qualifiedByName = "extraerNombreCompleto")
    AnuncioResumenResponse toResumenResponse(Anuncio entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "curso", ignore = true)
    @Mapping(target = "autor", ignore = true)
    @Mapping(target = "activo", ignore = true)
    Anuncio toEntity(CrearAnuncioRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "curso", ignore = true)
    @Mapping(target = "autor", ignore = true)
    @Mapping(target = "activo", ignore = true)
    void actualizarDesdeRequest(ActualizarAnuncioRequest request, @MappingTarget Anuncio entity);

    // Helpers

    @Named("extraerIdCurso")
    default UUID extraerIdCurso(Curso curso) {
        return curso == null ? null : curso.getId();
    }

    @Named("extraerTituloCurso")
    default String extraerTituloCurso(Curso curso) {
        return curso == null ? null : curso.getTitulo();
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
}