package com.garepas.garepasapp.web.view;

import java.math.BigDecimal;
import java.util.List;

public record CostoRecetaResponse(
        Long recetaId,
        String recetaNombre,
        BigDecimal costoTotal,
        BigDecimal costoUnitario,
        List<DetalleRecetaResponse> desglose
) {}
