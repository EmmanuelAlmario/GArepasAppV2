package com.garepas.garepasapp.dto.request;

import com.garepas.garepasapp.enums.CategoriaGasto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record GastoRequest(

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 150, message = "La descripción no puede superar 150 caracteres")
        String descripcion,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a cero")
        BigDecimal monto,

        @NotNull(message = "La categoría es obligatoria")
        CategoriaGasto categoria
) {}
