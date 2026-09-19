package com.kellyacademy.communication.mapper;

import com.kellyacademy.communication.dto.response.NotificacionResponse;
import com.kellyacademy.communication.dto.response.NotificacionResumenResponse;
import com.kellyacademy.communication.entity.Notificacion;
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
public interface NotificacionMapper {

    @Mapping(target = "usuarioId", source = "usuario", qualifiedByName = "extraerIdUsuario")
    NotificacionResponse toResponse(Notificacion entity);

    NotificacionResumenResponse toResumenResponse(Notificacion entity);

    // Helpers

    @Named("extraerIdUsuario")
    default UUID extraerIdUsuario(Usuario usuario) {
        return usuario == null ? null : usuario.getId();
    }
}