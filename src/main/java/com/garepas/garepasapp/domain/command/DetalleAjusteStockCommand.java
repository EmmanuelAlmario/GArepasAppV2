package com.garepas.garepasapp.domain.command;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Linea de un ajuste de inventario.
 *
 * <p>La cantidad es un delta en la unidad de presentacion del insumo.
 */
public record DetalleAjusteStockCommand(
        @NotNull(message = "El insumo es obligatorio")
        Long insumoId,
        @NotNull(message = "La cantidad es obligatoria")
        BigDecimal cantidad
) {
}
