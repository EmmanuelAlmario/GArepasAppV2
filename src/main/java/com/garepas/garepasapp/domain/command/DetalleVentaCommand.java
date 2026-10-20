package com.garepas.garepasapp.domain.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetalleVentaCommand(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero")
        Integer cantidad
) {
}
