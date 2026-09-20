package com.kellyacademy.user.service;

import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.user.dto.response.PermisoResponse;
import com.kellyacademy.user.dto.response.PermisoResumenResponse;
import com.kellyacademy.user.entity.Permiso;
import com.kellyacademy.user.mapper.RolMapper;
import com.kellyacademy.user.repository.PermisoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermisoService {

    private final PermisoRepository permisoRepository;
    private final RolMapper rolMapper;

    public Page<PermisoResumenResponse> listar(Pageable pageable) {
        return permisoRepository.findAll(pageable)
                .map(rolMapper::toPermisoResumenResponse);
    }

    public PermisoResponse obtener(UUID id) {
        Permiso permiso = permisoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso", "id", id));
        return rolMapper.toPermisoResponse(permiso);
    }
}