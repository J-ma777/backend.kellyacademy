package com.kellyacademy.enrollment.service;

import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transiciones y conservación del historial de una matrícula ya bloqueada por el llamador. */
@Service
@RequiredArgsConstructor
@Transactional
public class MatriculaEstadoService {
    private final CalculoMatriculaService calculo;

    public void cambiarEstado(Matricula matricula, EstadoMatricula nuevo) {
        EstadoMatricula actual = matricula.getEstado();
        if (actual == nuevo) {
            throw new BusinessException("ESTADO_SIN_CAMBIOS", "La matricula ya se encuentra en estado " + nuevo);
        }
        boolean permitida = switch (actual) {
            case ACTIVA -> nuevo == EstadoMatricula.COMPLETADA || nuevo == EstadoMatricula.RIESGO || nuevo == EstadoMatricula.ABANDONADA;
            case RIESGO -> nuevo == EstadoMatricula.ACTIVA || nuevo == EstadoMatricula.COMPLETADA || nuevo == EstadoMatricula.ABANDONADA;
            case COMPLETADA, ABANDONADA -> false;
        };
        if (!permitida) {
            throw new BusinessException("TRANSICION_ESTADO_INVALIDA", "No se puede pasar de " + actual + " a " + nuevo);
        }
        if (nuevo == EstadoMatricula.COMPLETADA || nuevo == EstadoMatricula.ABANDONADA) {
            // La matrícula todavía está activa al calcular su última instantánea.
            calculo.recalcular(matricula.getId());
        }
        matricula.setEstado(nuevo);
    }

    /** Retirar es idempotente para ABANDONADA; COMPLETADA conserva su historial. */
    public void retirar(Matricula matricula, String codigoCompletada, String mensajeCompletada) {
        if (matricula.getEstado() == EstadoMatricula.ABANDONADA) return;
        if (matricula.getEstado() == EstadoMatricula.COMPLETADA)
            throw new BusinessException(codigoCompletada, mensajeCompletada);
        cambiarEstado(matricula, EstadoMatricula.ABANDONADA);
    }
}
