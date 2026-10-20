package com.garepas.garepasapp.domain.command;

import com.garepas.garepasapp.domain.enums.UnidadMedida;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

  






public record DetalleRecetaCommand(
        @NotNull(message = "El insumo es obligatorio")
        Long insumoId,
        @NotNull(message = "La cantidad es obligatoria")
        BigDecimal cantidad,
        @NotNull(message = "La unidad es obligatoria")
        UnidadMedida unidad
) {
}
