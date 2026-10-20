package com.garepas.garepasapp.web.view;

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
