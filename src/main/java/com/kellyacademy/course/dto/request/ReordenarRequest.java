package com.kellyacademy.course.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReordenarRequest(
        @NotEmpty(message = "El orden no puede estar vacio")
        List<@Valid ItemOrden> orden
) {

    public record ItemOrden(
            @NotNull(message = "El ID es obligatorio")
            UUID id,

            @NotNull(message = "El numero es obligatorio")
            @Min(value = 1, message = "El numero debe ser mayor o igual a 1")
            Integer numero
    ) {
    }
}
