package com.garepas.garepasapp.domain.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductoCommand(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
        String nombre,
        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stockActual,
        @Min(value = 0, message = "El stock minimo no puede ser negativo")
        Integer stockMinimo,
        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio de venta debe ser mayor que cero")
        BigDecimal precioVenta,
        Long recetaId,
        Boolean bajoPedido,
        Boolean activo
) {
}
