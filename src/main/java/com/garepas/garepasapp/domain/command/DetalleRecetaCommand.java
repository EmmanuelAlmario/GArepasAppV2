package com.garepas.garepasapp.domain.command;

import com.garepas.garepasapp.domain.enums.UnidadMedida;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Ingrediente de una receta.
 *
 * <p>La cantidad llega en la unidad que el usuario escribio (kg, g, ml...) y
 * el servicio la convierte a la unidad base del insumo. La receta guarda
 * siempre cantidades en unidad base.
 */
public record DetalleRecetaCommand(
        @NotNull(message = "El insumo es obligatorio")
        Long insumoId,
        @NotNull(message = "La cantidad es obligatoria")
        BigDecimal cantidad,
        @NotNull(message = "La unidad es obligatoria")
        UnidadMedida unidad
) {
}
