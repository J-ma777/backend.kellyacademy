package com.kellyacademy.course.mapper;

import com.kellyacademy.course.dto.request.ActualizarCursoRequest;
import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.dto.response.CursoResumenResponse;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.mapper.UsuarioMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = { UsuarioMapper.class }
)
public interface CursoMapper {

    CursoResponse toResponse(Curso curso);

    @Mapping(target = "docenteId", source = "docente", qualifiedByName = "extraerId")
    CursoResumenResponse toResumenResponse(Curso curso);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "docente", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Curso toEntity(CrearCursoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "docente", ignore = true)
    @Mapping(target = "estado", ignore = true)
    void actualizarDesdeRequest(ActualizarCursoRequest request, @MappingTarget Curso curso);

    @Named("extraerId")
    default UUID extraerId(Usuario usuario) {
        return usuario == null ? null : usuario.getId();
    }
}