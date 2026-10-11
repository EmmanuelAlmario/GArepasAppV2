package com.garepas.garepasapp.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record DetalleAjusteStockRequest(

        @NotNull(message = "El id del insumo es obligatorio")
        Long insumoId,

        @NotNull(message = "La cantidad es obligatoria")
        BigDecimal cantidad
) {}