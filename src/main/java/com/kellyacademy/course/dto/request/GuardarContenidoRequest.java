package com.kellyacademy.course.dto.request;

import com.kellyacademy.course.enums.TipoActividad;
import com.kellyacademy.course.enums.TipoMaterial;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record GuardarContenidoRequest(
        @NotNull UUID semanaId,
        @NotBlank @Size(max = 200) String titulo,
        @Size(max = 5000) String descripcion,
        TipoMaterial tipo,
        @Size(max = 500) String urlArchivo,
        @Size(max = 500) String urlExterno,
        @DecimalMin("0") BigDecimal tamanoMb,
        @Size(max = 500) String urlGrabacion,
        LocalDateTime fechaHora,
        @Min(1) @Max(600) Integer duracionMinutos,
        @Size(max = 100) String sala,
        LocalDateTime fechaLimite,
        @Min(1) @Max(1000) Integer puntajeMaximo,
        TipoActividad tipoActividad,
        @NotNull Boolean publicado,
        @NotNull Boolean visible
) {}
