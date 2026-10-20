package com.garepas.garepasapp.domain.command;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

  




public record DetalleAjusteStockCommand(
        @NotNull(message = "El insumo es obligatorio")
        Long insumoId,
        @NotNull(message = "La cantidad es obligatoria")
        BigDecimal cantidad
) {
}
