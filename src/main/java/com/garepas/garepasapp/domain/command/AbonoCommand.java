package com.garepas.garepasapp.domain.command;

import com.garepas.garepasapp.domain.enums.FormaPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Abono recibido de una venta a credito.
 */
public record AbonoCommand(
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor que cero")
        BigDecimal monto,
        FormaPago metodo,
        @Size(max = 150, message = "La nota no puede superar los 150 caracteres")
        String nota
) {
}
