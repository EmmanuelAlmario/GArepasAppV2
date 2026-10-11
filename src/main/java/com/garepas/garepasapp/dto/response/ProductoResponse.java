package com.garepas.garepasapp.dto.response;

import com.garepas.garepasapp.entity.Producto;
import com.garepas.garepasapp.entity.Receta;
import com.garepas.garepasapp.service.CostoService;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record ProductoResponse(
        Long id,
        String nombre,
        Integer stockActual,
        Integer stockMinimo,
        Boolean stockBajo,
        BigDecimal precioVenta,
        Long recetaId,
        String recetaNombre,
        Boolean activo,
        BigDecimal costoUnitario,
        BigDecimal margen,
        BigDecimal margenPorcentaje
) {
    public static ProductoResponse desde(Producto producto) {
        Receta receta = producto.getReceta();
        BigDecimal costo = CostoService.costoDeReceta(receta);
        BigDecimal precioVenta = producto.getPrecioVenta() != null ? producto.getPrecioVenta() : BigDecimal.ZERO;
        BigDecimal margen = precioVenta.subtract(costo);
        BigDecimal margenPct = precioVenta.compareTo(BigDecimal.ZERO) > 0
                ? margen.multiply(BigDecimal.valueOf(100))
                        .divide(precioVenta, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Integer min = producto.getStockMinimo();
        boolean bajo = min != null && producto.getStockActual() != null && producto.getStockActual() < min;
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getStockActual(),
                min,
                bajo,
                precioVenta,
                receta != null ? receta.getId() : null,
                receta != null ? receta.getNombre() : null,
                producto.getActivo(),
                costo,
                margen,
                margenPct
        );
    }
}
