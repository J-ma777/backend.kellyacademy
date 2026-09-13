package com.kellyacademy.config;

import com.kellyacademy.model.enums.PermisoSistema;
import com.kellyacademy.model.usuario.Permiso;
import com.kellyacademy.model.usuario.Rol;
import com.kellyacademy.repository.PermisoRepository;
import com.kellyacademy.repository.RolRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final PermisoRepository permisoRepository;
    private final RolRepository rolRepository;

    @PostConstruct
    public void inicializarDatos() {

        if (rolRepository.count() > 0) {
            return;
        }

        Set<Permiso> permisos = crearPermisos();

        crearRolAdministrador(permisos);
        crearRolDocente(permisos);
        crearRolEstudiante(permisos);
    }

    private Set<Permiso> crearPermisos() {

        Set<Permiso> permisos = new HashSet<>();

        Arrays.stream(PermisoSistema.values())
                .forEach(valor -> {

                    Permiso permiso = new Permiso();
                    permiso.setNombre(valor.name());

                    permisos.add(
                            permisoRepository.save(permiso)
                    );
                });

        return permisos;
    }

    private void crearRolAdministrador(Set<Permiso> permisos) {

        Rol rol = new Rol();

        rol.setNombre("ADMINISTRADOR");
        rol.setDescripcion("Acceso total al sistema");
        rol.setPermisos(permisos);

        rolRepository.save(rol);
    }

    private void crearRolDocente(Set<Permiso> permisos) {

        Rol rol = new Rol();

        rol.setNombre("DOCENTE");
        rol.setDescripcion("Gestión académica");

        rol.setPermisos(
                permisos.stream()
                        .filter(p ->
                                p.getNombre().equals(PermisoSistema.CREAR_TAREA.name())
                                        || p.getNombre().equals(PermisoSistema.CALIFICAR_TAREA.name())
                                        || p.getNombre().equals(PermisoSistema.VER_CURSOS.name())
                                        || p.getNombre().equals(PermisoSistema.ENVIAR_MENSAJES.name()))
                        .collect(java.util.stream.Collectors.toSet())
        );

        rolRepository.save(rol);
    }

    private void crearRolEstudiante(Set<Permiso> permisos) {

        Rol rol = new Rol();

        rol.setNombre("ESTUDIANTE");
        rol.setDescripcion("Acceso estudiantil");

        rol.setPermisos(
                permisos.stream()
                        .filter(p ->
                                p.getNombre().equals(PermisoSistema.VER_CURSOS.name())
                                        || p.getNombre().equals(PermisoSistema.ENTREGAR_TAREA.name())
                                        || p.getNombre().equals(PermisoSistema.ENVIAR_MENSAJES.name()))
                        .collect(java.util.stream.Collectors.toSet())
        );

        rolRepository.save(rol);
    }
}