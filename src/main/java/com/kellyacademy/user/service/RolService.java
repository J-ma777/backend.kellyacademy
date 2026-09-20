package com.kellyacademy.user.service;

import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.user.dto.response.RolResponse;
import com.kellyacademy.user.dto.response.RolResumenResponse;
import com.kellyacademy.user.entity.Rol;
import com.kellyacademy.user.mapper.RolMapper;
import com.kellyacademy.user.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RolService {

    private final RolRepository rolRepository;
    private final RolMapper rolMapper;

    public Page<RolResumenResponse> listar(Pageable pageable) {
        // RolResumenResponse no incluye permisos: no cargamos la coleccion, evitamos N+1.
        return rolRepository.findAll(pageable)
                .map(rolMapper::toResumenResponse);
    }

    public RolResponse obtener(UUID id) {
        // RolResponse SI incluye permisos: @EntityGraph los carga en una query.
        Rol rol = rolRepository.findWithPermisosById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", "id", id));
        return rolMapper.toResponse(rol);
    }
}