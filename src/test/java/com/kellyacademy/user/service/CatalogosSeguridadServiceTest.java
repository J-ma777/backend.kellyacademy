package com.kellyacademy.user.service;

import com.kellyacademy.user.entity.*;
import com.kellyacademy.user.mapper.RolMapper;
import com.kellyacademy.user.repository.*;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogosSeguridadServiceTest {
    @Mock RolRepository roles; @Mock PermisoRepository permisos;
    private final RolMapper mapper = Mappers.getMapper(RolMapper.class);
    @Test void detalleDeRolConservaPermisosOrdenadosYNoPierdeIdentificadores() {
        var rol = new Rol(); rol.setId(UUID.randomUUID()); rol.setNombre("DOCENTE");
        var a = new Permiso(); a.setId(UUID.randomUUID()); a.setNombre("ANUNCIO_CREAR");
        var b = new Permiso(); b.setId(UUID.randomUUID()); b.setNombre("CURSO_LEER"); rol.setPermisos(Set.of(b, a));
        when(roles.findWithPermisosById(rol.getId())).thenReturn(Optional.of(rol));
        var result = new RolService(roles, mapper).obtener(rol.getId());
        assertThat(result.id()).isEqualTo(rol.getId()); assertThat(result.permisos()).extracting("nombre").containsExactly("ANUNCIO_CREAR", "CURSO_LEER");
    }
    @Test void catalogoDeRolesConservaLaPaginacion() {
        var page = PageRequest.of(1, 5); var rol = new Rol(); rol.setNombre("DOCENTE");
        when(roles.findAll(page)).thenReturn(new PageImpl<>(List.of(rol), page, 6));
        var result = new RolService(roles, mapper).listar(page);
        assertThat(result.getNumber()).isEqualTo(1); assertThat(result.getTotalElements()).isEqualTo(6);
        assertThat(result.getContent()).extracting("nombre").containsExactly("DOCENTE");
    }
    @Test void detalleDePermisoConservaNombreYDescripcion() {
        var permiso = new Permiso(); permiso.setId(UUID.randomUUID()); permiso.setNombre("CURSO_LEER"); permiso.setDescripcion("Lectura del curso");
        when(permisos.findById(permiso.getId())).thenReturn(Optional.of(permiso));
        var result = new PermisoService(permisos, mapper).obtener(permiso.getId());
        assertThat(result.id()).isEqualTo(permiso.getId()); assertThat(result.descripcion()).isEqualTo("Lectura del curso");
    }
    @Test void catalogosInexistentesDevuelvenErrorDeDominio() {
        assertThatThrownBy(() -> new RolService(roles, mapper).obtener(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> new PermisoService(permisos, mapper).obtener(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }
}
