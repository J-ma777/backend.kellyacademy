package com.kellyacademy.course.dto.request;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
public record HorarioCursoRequest(@NotNull DayOfWeek dia,@NotNull LocalTime horaInicio,@NotNull LocalTime horaFin) {}
