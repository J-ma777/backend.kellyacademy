package com.kellyacademy.enrollment.mapper;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.enrollment.dto.request.CrearMatriculaRequest;
import com.kellyacademy.enrollment.dto.response.MatriculaResponse;
import com.kellyacademy.enrollment.dto.response.MatriculaResumenResponse;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.user.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MatriculaMapper {

    @Mapping(target = "cursoId", source = "curso", qualifiedByName = "extraerIdCurso")
    @Mapping(target = "cursoTitulo", source = "curso", qualifiedByName = "extraerTituloCurso")
    @Mapping(target = "estudianteId", source = "estudiante", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "estudianteNombreCompleto", source = "estudiante", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "estudianteCorreo", source = "estudiante", qualifiedByName = "extraerCorreo")
    MatriculaResponse toResponse(Matricula entity);

    @Mapping(target = "cursoId", source = "curso", qualifiedByName = "extraerIdCurso")
    @Mapping(target = "cursoTitulo", source = "curso", qualifiedByName = "extraerTituloCurso")
    @Mapping(target = "estudianteId", source = "estudiante", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "estudianteNombreCompleto", source = "estudiante", qualifiedByName = "extraerNombreCompleto")
    MatriculaResumenResponse toResumenResponse(Matricula entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "curso", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "notaFinal", ignore = true)
    @Mapping(target = "asistenciaPorcentaje", ignore = true)
    @Mapping(target = "matriculadoAt", ignore = true)
    Matricula toEntity(CrearMatriculaRequest request);

    // Helpers: extraen campos planos sin disparar SELECTs adicionales

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

    @Named("extraerCorreo")
    default String extraerCorreo(Usuario usuario) {
        return usuario == null ? null : usuario.getCorreoElectronico();
    }
}