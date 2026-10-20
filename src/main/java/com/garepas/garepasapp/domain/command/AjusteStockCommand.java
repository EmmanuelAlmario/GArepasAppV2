package com.garepas.garepasapp.domain.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Ajuste de inventario: varias lineas, atomicas (se aplican todas o ninguna).
 *
 * <p>La cantidad es un DELTA en la unidad de presentacion del insumo:
 * positiva repone (compra), negativa consume (merma).
 */
public record AjusteStockCommand(
        @NotEmpty(message = "Debe incluir al menos un insumo para ajustar")
        @Valid
        List<DetalleAjusteStockCommand> detalles
) {
}
