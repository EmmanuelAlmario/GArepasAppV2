package com.garepas.garepasapp.domain.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

  






public record RecetaCommand(
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
        String nombre,
        @Size(max = 150, message = "La descripcion no puede superar los 150 caracteres")
        String descripcion,
        @NotEmpty(message = "La receta debe incluir al menos un ingrediente")
        @Valid
        List<DetalleRecetaCommand> detalles,
        Long insumoProducidoId,
        BigDecimal rendimiento
) {
}
