package com.garepas.garepasapp.dto.request;

import com.garepas.garepasapp.enums.UnidadMedida;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record DetalleRecetaRequest(

        @NotNull(message = "El id del insumo es obligatorio")
        Long insumoId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad debe ser mayor a cero")
        BigDecimal cantidad,

        @NotNull(message = "La unidad de medida es obligatoria")
        UnidadMedida unidadMedida
) {}
