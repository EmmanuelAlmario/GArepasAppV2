package com.garepas.garepasapp.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CostoRecetaResponse(
        Long recetaId,
        String recetaNombre,
        BigDecimal costoTotal,
        List<DetalleRecetaResponse> desglose
) {}
