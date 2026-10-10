package com.kellyacademy.attendance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/*
 Request para el registro masivo de asistencia de una clase.
 Endpoint dedicado: POST /api/asistencias/masivo.

 Se valida previamente TODO el batch antes de persistir: si algun registro
 es invalido (estudiante inexistente, sin rol ESTUDIANTE, no matriculado,
 asistencia duplicada), se rechaza la operacion completa con un mapa de
 errores por estudiante. No hay persistencia parcial.
 */
public record CrearAsistenciasMasivasRequest(

        @NotNull(message = "El ID de la clase es obligatorio")
        UUID claseId,

        @NotEmpty(message = "Debe incluir al menos un registro de asistencia")
        @Valid
        List<RegistroAsistenciaItemRequest> registros
) {
}