package com.kellyacademy.config;

import com.kellyacademy.model.enums.EstadoUsuario;
import com.kellyacademy.model.enums.PermisoSistema;
import com.kellyacademy.model.usuario.Permiso;
import com.kellyacademy.model.usuario.Rol;
import com.kellyacademy.model.usuario.Usuario;
import com.kellyacademy.repository.PermisoRepository;
import com.kellyacademy.repository.RolRepository;
import com.kellyacademy.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final PermisoRepository permisoRepository;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;


    @PostConstruct
    public void inicializarDatos() {

        if (rolRepository.count() == 0) {

            Set<Permiso> permisos = crearPermisos();

            crearRolAdministrador(permisos);
            crearRolDocente(permisos);
            crearRolEstudiante(permisos);
        }

        crearAdministrador();
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

    private void crearAdministrador() {

        if (usuarioRepository.existsByCorreoElectronico(
                "admin@kellyacademy.com"
        )) {
            return;
        }

        Rol rolAdministrador = rolRepository
                .findByNombre("ADMINISTRADOR")
                .orElseThrow();

        Usuario usuario = new Usuario();

        usuario.setNombre("Administrador");
        usuario.setApellido("Sistema");
        usuario.setCorreoElectronico("admin@kellyacademy.com");

        usuario.setContrasena(
                passwordEncoder.encode("Admin123*")
        );

        usuario.setEstado(
                EstadoUsuario.ACTIVO
        );

        usuario.setRoles(
                Set.of(rolAdministrador)
        );

        usuarioRepository.save(usuario);
    }
}