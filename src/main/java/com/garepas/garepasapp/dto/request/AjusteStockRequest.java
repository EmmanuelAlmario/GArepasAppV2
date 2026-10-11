package com.garepas.garepasapp.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AjusteStockRequest(

        @NotEmpty(message = "Debe incluir al menos un insumo para ajustar")
        @Valid
        List<DetalleAjusteStockRequest> detalles
) {}
