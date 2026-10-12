package com.kellyacademy.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DashboardResumenResponse(
        MetricasDto metricas,
        List<ClaseHoyDto> clasesHoy,
        List<TareaPendienteDto> tareasPendientes,
        String rolUsuario,
        String nombreUsuario
) {
    public record MetricasDto(
            long cursosInscritos,
            BigDecimal promedioCalificacion,
            long tareasEntregadas,
            BigDecimal porcentajeAsistencia,
            double horasEstudioSemanal
    ) {}

    public record ClaseHoyDto(
            UUID id,
            String titulo,
            String cursoTitulo,
            String horario,
            String docenteNombre,
            String enlaceVirtual,
            boolean enVivo
    ) {}

    public record TareaPendienteDto(
            UUID id,
            String titulo,
            String cursoTitulo,
            String fechaLimite,
            boolean urgente,
            String estado
    ) {}
}
