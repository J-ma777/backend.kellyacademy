package com.kellyacademy.communication.mapper;

import com.kellyacademy.communication.dto.request.CrearConversacionRequest;
import com.kellyacademy.communication.dto.response.ConversacionResponse;
import com.kellyacademy.communication.dto.response.ConversacionResumenResponse;
import com.kellyacademy.communication.entity.Conversacion;
import com.kellyacademy.course.entity.Curso;
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
public interface ConversacionMapper {

    // --- Response detalle: sin conteo de no leidos ---

    @Mapping(target = "cursoId", source = "curso", qualifiedByName = "extraerIdCurso")
    @Mapping(target = "cursoTitulo", source = "curso", qualifiedByName = "extraerTituloCurso")
    @Mapping(target = "participante1Id", source = "participante1", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "participante1NombreCompleto", source = "participante1", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "participante2Id", source = "participante2", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "participante2NombreCompleto", source = "participante2", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "mensajesNoLeidos", ignore = true)
    ConversacionResponse toResponse(Conversacion entity);

    // --- Response detalle: con conteo de no leidos (el servicio lo provee) ---

    @Mapping(target = "cursoId", source = "entity.curso", qualifiedByName = "extraerIdCurso")
    @Mapping(target = "cursoTitulo", source = "entity.curso", qualifiedByName = "extraerTituloCurso")
    @Mapping(target = "participante1Id", source = "entity.participante1", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "participante1NombreCompleto", source = "entity.participante1", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "participante2Id", source = "entity.participante2", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "participante2NombreCompleto", source = "entity.participante2", qualifiedByName = "extraerNombreCompleto")
    ConversacionResponse toResponse(Conversacion entity, long mensajesNoLeidos);

    // --- Response resumen: sin conteo ---

    @Mapping(target = "cursoId", source = "curso", qualifiedByName = "extraerIdCurso")
    @Mapping(target = "cursoTitulo", source = "curso", qualifiedByName = "extraerTituloCurso")
    @Mapping(target = "participante1Id", source = "participante1", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "participante1NombreCompleto", source = "participante1", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "participante2Id", source = "participante2", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "participante2NombreCompleto", source = "participante2", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "mensajesNoLeidos", ignore = true)
    ConversacionResumenResponse toResumenResponse(Conversacion entity);

    // --- Response resumen: con conteo ---

    @Mapping(target = "cursoId", source = "entity.curso", qualifiedByName = "extraerIdCurso")
    @Mapping(target = "cursoTitulo", source = "entity.curso", qualifiedByName = "extraerTituloCurso")
    @Mapping(target = "participante1Id", source = "entity.participante1", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "participante1NombreCompleto", source = "entity.participante1", qualifiedByName = "extraerNombreCompleto")
    @Mapping(target = "participante2Id", source = "entity.participante2", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "participante2NombreCompleto", source = "entity.participante2", qualifiedByName = "extraerNombreCompleto")
    ConversacionResumenResponse toResumenResponse(Conversacion entity, long mensajesNoLeidos);

    // --- Entity desde request ---
    // El servicio resuelve y setea participante1, participante2, curso y ultimoMensajeAt.

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "curso", ignore = true)
    @Mapping(target = "participante1", ignore = true)
    @Mapping(target = "participante2", ignore = true)
    @Mapping(target = "ultimoMensajeAt", ignore = true)
    Conversacion toEntity(CrearConversacionRequest request);

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