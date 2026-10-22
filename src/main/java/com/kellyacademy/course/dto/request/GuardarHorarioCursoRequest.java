package com.kellyacademy.course.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public record GuardarHorarioCursoRequest(@NotNull LocalDate fechaInicio, LocalDate fechaFin,
        @NotEmpty @Size(max=21) List<@NotNull @Valid HorarioCursoRequest> horarios) {}
