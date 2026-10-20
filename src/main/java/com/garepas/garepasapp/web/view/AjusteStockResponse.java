package com.garepas.garepasapp.web.view;

import java.math.BigDecimal;
import java.util.List;

public record AjusteStockResponse(
        int cantidadInsumosAjustados,
        BigDecimal valorTotalAjustado,
        List<InsumoResponse> insumosActualizados
) {}
