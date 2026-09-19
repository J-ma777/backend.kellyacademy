package com.kellyacademy.communication.mapper;

import com.kellyacademy.communication.dto.request.CrearMensajeRequest;
import com.kellyacademy.communication.dto.response.MensajeResponse;
import com.kellyacademy.communication.dto.response.MensajeResumenResponse;
import com.kellyacademy.communication.entity.Conversacion;
import com.kellyacademy.communication.entity.Mensaje;
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
public interface MensajeMapper {

    @Mapping(target = "conversacionId", source = "conversacion", qualifiedByName = "extraerIdConversacion")
    @Mapping(target = "remitenteId", source = "remitente", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "remitenteNombreCompleto", source = "remitente", qualifiedByName = "extraerNombreCompleto")
    MensajeResponse toResponse(Mensaje entity);

    @Mapping(target = "conversacionId", source = "conversacion", qualifiedByName = "extraerIdConversacion")
    @Mapping(target = "remitenteId", source = "remitente", qualifiedByName = "extraerIdUsuario")
    @Mapping(target = "remitenteNombreCompleto", source = "remitente", qualifiedByName = "extraerNombreCompleto")
    MensajeResumenResponse toResumenResponse(Mensaje entity);

    // El servicio setea: conversacion, remitente, enviadoAt, leido.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "conversacion", ignore = true)
    @Mapping(target = "remitente", ignore = true)
    @Mapping(target = "leido", ignore = true)
    @Mapping(target = "enviadoAt", ignore = true)
    Mensaje toEntity(CrearMensajeRequest request);

    // Helpers

    @Named("extraerIdConversacion")
    default UUID extraerIdConversacion(Conversacion conversacion) {
        return conversacion == null ? null : conversacion.getId();
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