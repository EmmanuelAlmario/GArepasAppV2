package com.garepas.garepasapp.dto.response;

import java.math.BigDecimal;

public record CostoProductoResponse(
        Long productoId,
        String productoNombre,
        Long recetaId,
        BigDecimal costoUnitario,
        BigDecimal precioVenta,
        BigDecimal margen,
        BigDecimal margenPorcentaje
) {}
