package com.garepas.garepasapp.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AjusteStockResponse(
        int cantidadInsumosAjustados,
        BigDecimal valorTotalAjustado,
        List<InsumoResponse> insumosActualizados
) {}
