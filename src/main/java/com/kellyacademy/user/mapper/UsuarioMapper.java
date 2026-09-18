package com.kellyacademy.user.mapper;

import com.kellyacademy.user.dto.request.ActualizarUsuarioRequest;
import com.kellyacademy.user.dto.request.CrearUsuarioRequest;
import com.kellyacademy.user.dto.response.UsuarioResumenResponse;
import com.kellyacademy.user.dto.response.UsuarioResponse;
import com.kellyacademy.user.entity.Rol;
import com.kellyacademy.user.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UsuarioMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "nombresDeRoles")
    UsuarioResponse toResponse(Usuario usuario);

    UsuarioResumenResponse toResumenResponse(Usuario usuario);

    // contrasena, estado y roles los resuelve el servicio antes de persistir.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "contrasena", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "roles", ignore = true)
    Usuario toEntity(CrearUsuarioRequest request);

    // Update parcial: solo pisa los campos del request, preserva el resto.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "contrasena", ignore = true)
    @Mapping(target = "correoElectronico", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "roles", ignore = true)
    void actualizarDesdeRequest(ActualizarUsuarioRequest request, @MappingTarget Usuario usuario);

    @Named("nombresDeRoles")
    default List<String> nombresDeRoles(Set<Rol> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(Rol::getNombre)
                .sorted()
                .collect(Collectors.toList());
    }
}