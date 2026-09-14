package com.kellyacademy.infrastructure.seeder;

import com.kellyacademy.user.entity.Permiso;
import com.kellyacademy.user.entity.Rol;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.enums.EstadoUsuario;
import com.kellyacademy.user.enums.PermisoSistema;
import com.kellyacademy.user.repository.PermisoRepository;
import com.kellyacademy.user.repository.RolRepository;
import com.kellyacademy.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.seeder.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DataSeeder implements ApplicationRunner {

    private final PermisoRepository permisoRepository;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        log.info("Iniciando verificacion de datos semilla...");

        if (rolRepository.count() == 0) {
            log.info("No existen roles. Creando permisos y roles iniciales...");

            Set<Permiso> permisos = crearPermisos();
            crearRolAdministrador(permisos);
            crearRolDocente(permisos);
            crearRolEstudiante(permisos);

            log.info("Permisos y roles creados exitosamente.");

        } else {
            log.info("Roles ya existentes. Saltando creacion de permisos y roles.");
        }

        crearAdministrador();

        log.info("Verificacion de datos semilla finalizada.");
    }

    private Set<Permiso> crearPermisos() {

        Set<Permiso> permisos = new HashSet<>();

        Arrays.stream(PermisoSistema.values())
                .forEach(valor -> {

                    Permiso permiso = new Permiso();
                    permiso.setNombre(valor.name());

                    permisos.add(permisoRepository.save(permiso));
                });

        return permisos;
    }

    private void crearRolAdministrador(Set<Permiso> permisos) {

        Rol rol = new Rol();

        rol.setNombre("ADMINISTRADOR");
        rol.setDescripcion("Acceso total al sistema");
        rol.setPermisos(permisos);

        rolRepository.save(rol);

        log.info("Rol ADMINISTRADOR creado con {} permisos", permisos.size());
    }

    private void crearRolDocente(Set<Permiso> permisos) {

        Rol rol = new Rol();

        rol.setNombre("DOCENTE");
        rol.setDescripcion("Gestion academica");

        rol.setPermisos(
                permisos.stream()
                        .filter(p ->
                                p.getNombre().equals(PermisoSistema.CREAR_TAREA.name())
                                        || p.getNombre().equals(PermisoSistema.CALIFICAR_TAREA.name())
                                        || p.getNombre().equals(PermisoSistema.VER_CURSOS.name())
                                        || p.getNombre().equals(PermisoSistema.ENVIAR_MENSAJES.name()))
                        .collect(Collectors.toSet())
        );

        rolRepository.save(rol);

        log.info("Rol DOCENTE creado con {} permisos", rol.getPermisos().size());
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
                        .collect(Collectors.toSet())
        );

        rolRepository.save(rol);

        log.info("Rol ESTUDIANTE creado con {} permisos", rol.getPermisos().size());
    }

    private void crearAdministrador() {

        String correoAdmin = "admin@kellyacademy.com";

        if (usuarioRepository.existsByCorreoElectronico(correoAdmin)) {
            log.info("Usuario administrador ya existe. Saltando creacion.");
            return;
        }

        log.info("Creando usuario administrador inicial...");

        Rol rolAdministrador = rolRepository
                .findByNombre("ADMINISTRADOR")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Rol ADMINISTRADOR no encontrado. No se puede crear el administrador."
                        )
                );

        Usuario usuario = new Usuario();

        usuario.setNombre("Administrador");
        usuario.setApellido("Sistema");
        usuario.setCorreoElectronico(correoAdmin);
        usuario.setContrasena(passwordEncoder.encode("Admin123*"));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setRoles(Set.of(rolAdministrador));

        usuarioRepository.save(usuario);

        log.info("Usuario administrador creado: {}", correoAdmin);
    }
}