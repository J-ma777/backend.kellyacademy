package com.kellyacademy.user.service;

import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.CorreoYaRegistradoException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.shared.util.SecurityUtils;
import com.kellyacademy.user.dto.request.ActualizarUsuarioRequest;
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

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private static final String RECURSO = "Usuario";

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

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        usuarioRepository.delete(usuario);
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