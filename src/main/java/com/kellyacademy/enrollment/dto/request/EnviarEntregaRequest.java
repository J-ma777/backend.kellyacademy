package com.kellyacademy.enrollment.dto.request;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record EnviarEntregaRequest(@NotNull UUID tareaId, @Size(max = 500) String urlArchivo,
                                  @Size(max = 20000) String textoRespuesta) {}
