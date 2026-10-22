package com.kellyacademy.course.dto.response;

import com.kellyacademy.communication.dto.response.AnuncioResponse;
import com.kellyacademy.enrollment.dto.response.EntregaResponse;
import com.kellyacademy.enrollment.dto.response.MatriculaResponse;
import java.util.List;

/** Vista de gestión: solo el docente propietario o un administrador. */
public record PanelCursoResponse(
        CursoResponse curso,
        List<UnidadPanel> unidades,
        List<MatriculaResponse> matriculas,
        List<AnuncioResponse> anuncios,
        List<EntregaResponse> entregas,
        boolean puedeGestionar,
        List<AvanceAlumno> avances,
        Estadisticas estadisticas,
        List<java.util.UUID> contenidosCompletados
) {
    public record AvanceAlumno(java.util.UUID estudianteId, String avatarUrl, java.math.BigDecimal progreso) {}
    public record Estadisticas(long totalAlumnos, long alumnosActivos, java.math.BigDecimal progresoPromedio, long recursosPublicados) {}
    public record UnidadPanel(UnidadResponse unidad, List<SemanaPanel> semanas) {}
    public record SemanaPanel(
            SemanaResponse semana,
            List<ClaseResponse> clases,
            List<MaterialResponse> materiales,
            List<TareaResponse> tareas
    ) {}
}
