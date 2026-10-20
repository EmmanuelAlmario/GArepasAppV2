package com.garepas.garepasapp.domain.command;

import com.garepas.garepasapp.domain.enums.CategoriaGasto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

  



public record GastoCommand(
        @NotBlank(message = "La descripcion es obligatoria")
        @Size(max = 150, message = "La descripcion no puede superar los 150 caracteres")
        String descripcion,
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor que cero")
        BigDecimal monto,
        @NotNull(message = "La categoria es obligatoria")
        CategoriaGasto categoria,
        LocalDateTime fecha
) {
}
