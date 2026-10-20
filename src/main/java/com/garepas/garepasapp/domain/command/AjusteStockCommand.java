package com.garepas.garepasapp.domain.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.util.List;

  





public record AjusteStockCommand(
        @NotEmpty(message = "Debe incluir al menos un insumo para ajustar")
        @Valid
        List<DetalleAjusteStockCommand> detalles
) {
}
