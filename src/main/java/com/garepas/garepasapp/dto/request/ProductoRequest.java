package com.garepas.garepasapp.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductoRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre no puede superar 50 caracteres")
        String nombre,

        @NotNull(message = "El stock actual es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stockActual,

        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a cero")
        BigDecimal precioVenta,

        Long recetaId,

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {}
