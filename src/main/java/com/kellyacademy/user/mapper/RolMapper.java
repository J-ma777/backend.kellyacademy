package com.kellyacademy.user.mapper;

import com.kellyacademy.user.dto.response.PermisoResponse;
import com.kellyacademy.user.dto.response.PermisoResumenResponse;
import com.kellyacademy.user.dto.response.RolResponse;
import com.kellyacademy.user.dto.response.RolResumenResponse;
import com.kellyacademy.user.entity.Permiso;
import com.kellyacademy.user.entity.Rol;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface RolMapper {

    @Mapping(target = "permisos", source = "permisos", qualifiedByName = "permisosOrdenados")
    RolResponse toResponse(Rol rol);

    RolResumenResponse toResumenResponse(Rol rol);

    PermisoResponse toPermisoResponse(Permiso permiso);

    PermisoResumenResponse toPermisoResumenResponse(Permiso permiso);

    @Named("permisosOrdenados")
    default List<PermisoResumenResponse> permisosOrdenados(Set<Permiso> permisos) {
        if (permisos == null) {
            return List.of();
        }
        return permisos.stream()
                .map(this::toPermisoResumenResponse)
                .sorted((a, b) -> a.nombre().compareTo(b.nombre()))
                .collect(Collectors.toList());
    }
}