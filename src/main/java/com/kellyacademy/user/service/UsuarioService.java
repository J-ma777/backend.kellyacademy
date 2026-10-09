package com.kellyacademy.user.service;

import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.CorreoYaRegistradoException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.shared.util.SecurityUtils;
import com.kellyacademy.user.dto.request.ActualizarUsuarioRequest;
import com.kellyacademy.user.dto.request.CambiarContrasenaRequest;
import com.kellyacademy.user.dto.request.CrearUsuarioRequest;
import com.kellyacademy.user.dto.response.UsuarioResponse;
import com.kellyacademy.user.dto.response.UsuarioResumenResponse;
import com.kellyacademy.user.entity.Rol;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.enums.EstadoUsuario;
import com.kellyacademy.user.mapper.UsuarioMapper;
import com.kellyacademy.user.repository.RolRepository;
import com.kellyacademy.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private static final String RECURSO = "Usuario";
    private static final String ROL_ADMIN = "ADMINISTRADOR";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UsuarioResumenResponse> listar(Pageable pageable) {
        // UsuarioResumenResponse no incluye roles: no cargamos colecciones, evitamos N+1.
        return usuarioRepository.findAll(pageable)
                .map(usuarioMapper::toResumenResponse);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(UUID id) {
        // Autorizacion fina: propio o admin. El controller solo exige autenticacion.
        if (!SecurityUtils.esAdmin() && !SecurityUtils.esElMismoUsuario(id)) {
            throw new AccessDeniedException("No tienes permisos para consultar este usuario");
        }

        Usuario usuario = usuarioRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        return usuarioMapper.toResponse(usuario);
    }

    public UsuarioResponse crear(CrearUsuarioRequest request) {

        if (usuarioRepository.existsByCorreoElectronico(request.correoElectronico())) {
            throw new CorreoYaRegistradoException(request.correoElectronico());
        }

        Set<Rol> roles = resolverRoles(request.roles());

        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setRoles(roles);

        Usuario guardado = usuarioRepository.save(usuario);

        // Recargamos con @EntityGraph para devolver la respuesta con roles ya cargados.
        return usuarioMapper.toResponse(
                usuarioRepository.findWithRolesById(guardado.getId()).orElseThrow()
        );
    }

    public UsuarioResponse actualizar(UUID id, ActualizarUsuarioRequest request) {

        if (!SecurityUtils.esAdmin() && !SecurityUtils.esElMismoUsuario(id)) {
            throw new AccessDeniedException("No tienes permisos para modificar este usuario");
        }

        Usuario usuario = usuarioRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        usuarioMapper.actualizarDesdeRequest(request, usuario);

        return usuarioMapper.toResponse(usuario);
    }

    public void eliminar(UUID id) {

        // Un admin no puede borrarse a si mismo: se quedaria sin sesion y sin poder revertirlo.
        if (SecurityUtils.esElMismoUsuario(id)) {
            throw new BusinessException(
                    "NO_PUEDE_AUTODELETARSE",
                    "Un administrador no puede eliminar su propia cuenta"
            );
        }

        // Cargamos con roles porque la proteccion del ultimo admin activo
        // necesita inspeccionar el grafo de roles del objetivo.
        Usuario usuario = usuarioRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        // Un delete implica que el usuario deja de existir: nunca sera admin activo despues.
        validarNoEsUltimoAdminActivo(
                usuario,
                false,
                "No se puede eliminar al ultimo administrador activo del sistema."
        );

        usuarioRepository.delete(usuario);
    }

    public UsuarioResponse cambiarEstado(UUID id, EstadoUsuario nuevoEstado) {

        // Un admin no puede cambiarse el estado a si mismo: se bloquearia la sesion actual
        // y no podria revertirlo. Mismo principio que eliminar().
        if (SecurityUtils.esElMismoUsuario(id)) {
            throw new BusinessException(
                    "NO_PUEDE_AUTOCAMBIAR_ESTADO",
                    "Un administrador no puede cambiar su propio estado"
            );
        }

        Usuario usuario = usuarioRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        if (usuario.getEstado() == nuevoEstado) {
            throw new BusinessException(
                    "ESTADO_SIN_CAMBIOS",
                    "El usuario ya se encuentra en estado " + nuevoEstado
            );
        }

        // El usuario seguira siendo admin activo solo si el nuevo estado es ACTIVO.
        // (Aunque tenga el rol ADMINISTRADOR, un estado distinto a ACTIVO lo desactiva.)
        boolean seguiraSiendoAdminActivo = (nuevoEstado == EstadoUsuario.ACTIVO);
        validarNoEsUltimoAdminActivo(
                usuario,
                seguiraSiendoAdminActivo,
                "No se puede desactivar o bloquear al ultimo administrador activo del sistema."
        );

        usuario.setEstado(nuevoEstado);

        return usuarioMapper.toResponse(usuario);
    }

    public UsuarioResponse asignarRoles(UUID id, Set<String> nombresRoles) {

        // Un admin no puede modificarse sus propios roles: podria quitarse
        // ADMINISTRADOR y quedarse sin acceso administrativo para revertirlo.
        if (SecurityUtils.esElMismoUsuario(id)) {
            throw new BusinessException(
                    "NO_PUEDE_AUTOCAMBIAR_ROLES",
                    "Un administrador no puede modificar sus propios roles"
            );
        }

        Usuario usuario = usuarioRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        // Resolvemos primero: si algun rol no existe, queremos ROL_INEXISTENTE
        // antes que ULTIMO_ADMIN_ACTIVO (es un error de datos mas especifico).
        Set<Rol> roles = resolverRoles(nombresRoles);

        // Tras el reemplazo total, seguira siendo admin activo solo si el nuevo
        // set incluye ADMINISTRADOR y su estado actual ya es ACTIVO.
        boolean seguiraSiendoAdminActivo = nombresRoles.contains(ROL_ADMIN);
        validarNoEsUltimoAdminActivo(
                usuario,
                seguiraSiendoAdminActivo,
                "No se puede quitar el rol ADMINISTRADOR al ultimo administrador activo del sistema."
        );

        usuario.setRoles(roles);

        return usuarioMapper.toResponse(usuario);
    }

    // Cambia la contrasena del propio usuario autenticado.
    // Requiere conocer la actual como prueba de identidad (defensa en profundidad
    // contra tokens robados). No hay re-hash si la nueva es identica a la actual.
    public void cambiarContrasena(UUID id, CambiarContrasenaRequest request) {

        // Solo el propio usuario. Un ADMIN no puede forzar reset por este endpoint
        // (no conoce la contrasena actual). Si se necesita reset admin, va aparte.
        if (!SecurityUtils.esElMismoUsuario(id)) {
            throw new AccessDeniedException("No tienes permisos para cambiar la contrasena de este usuario");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        // Validacion de contrasena actual contra el hash almacenado.
        if (!passwordEncoder.matches(request.contrasenaActual(), usuario.getContrasena())) {
            throw new BusinessException(
                    "CONTRASENA_ACTUAL_INCORRECTA",
                    "La contrasena actual no es correcta"
            );
        }

        // No-op: la nueva no puede ser igual a la actual.
        // Se compara en claro (ambas vienen del request / se acaba de validar la actual).
        if (request.contrasenaNueva().equals(request.contrasenaActual())) {
            throw new BusinessException(
                    "CONTRASENA_SIN_CAMBIOS",
                    "La contrasena nueva debe ser distinta a la actual"
            );
        }

        usuario.setContrasena(passwordEncoder.encode(request.contrasenaNueva()));
    }

    // ------------------------------------------------------------------------
    // PROTECCION DEL ULTIMO ADMIN ACTIVO (#70)
    // ------------------------------------------------------------------------

    // Un usuario es "admin activo" si tiene el rol ADMINISTRADOR y estado ACTIVO.
    // Un admin INACTIVO o BLOQUEADO no cuenta como admin operativo.
    private boolean esAdminActivo(Usuario usuario) {
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            return false;
        }
        Set<Rol> roles = usuario.getRoles();
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(r -> ROL_ADMIN.equals(r.getNombre()));
    }

    // Si el usuario es admin activo y la operacion lo dejaria sin esa condicion,
    // valida que quede al menos otro admin activo en el sistema. Si es el ultimo,
    // rechaza la operacion con ULTIMO_ADMIN_ACTIVO.
    //
    // Si el usuario no era admin activo, o si seguira siendo admin activo tras
    // la operacion, no hay nada que validar.
    private void validarNoEsUltimoAdminActivo(Usuario usuario,
                                              boolean seguiraSiendoAdminActivo,
                                              String mensaje) {
        if (seguiraSiendoAdminActivo || !esAdminActivo(usuario)) {
            return;
        }
        long adminsActivos = usuarioRepository.countByRolNombreAndEstado(
                ROL_ADMIN, EstadoUsuario.ACTIVO
        );
        if (adminsActivos <= 1) {
            throw new BusinessException("ULTIMO_ADMIN_ACTIVO", mensaje);
        }
    }

    // Resuelve los nombres de roles del request a entidades. Falla si alguno no existe.
    // No creamos roles al vuelo: los roles del sistema son fijos y administrados.
    private Set<Rol> resolverRoles(Set<String> nombres) {
        Set<Rol> roles = new HashSet<>();
        for (String nombre : nombres) {
            Rol rol = rolRepository.findByNombre(nombre)
                    .orElseThrow(() -> new BusinessException(
                            "ROL_INEXISTENTE",
                            "Rol no encontrado: " + nombre
                    ));
            roles.add(rol);
        }
        return roles;
    }
}