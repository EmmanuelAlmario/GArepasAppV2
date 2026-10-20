package com.garepas.garepasapp.web.view;

import java.math.BigDecimal;

/**
 * Costo de un producto: costo por unidad (ya dividido por el rendimiento de
 * la receta), precio de venta y margen.
 */
public record CostoProductoResponse(
        Long productoId,
        String productoNombre,
        Long recetaId,
        BigDecimal costoUnitario,
        BigDecimal precioVenta,
        BigDecimal margen,
        BigDecimal margenPorcentaje
) {}
