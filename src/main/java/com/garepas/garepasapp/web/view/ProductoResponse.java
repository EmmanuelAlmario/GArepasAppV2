package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.model.Producto;
import com.garepas.garepasapp.domain.model.Receta;
import com.garepas.garepasapp.domain.service.CostoService;

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
        Boolean bajoPedido,
        Boolean activo,
        BigDecimal costoUnitario,
        BigDecimal margen,
        BigDecimal margenPorcentaje
) {
    public static ProductoResponse desde(Producto producto) {
        Receta receta = producto.getReceta();
          
        BigDecimal costo = CostoService.costoUnitarioProducto(producto);
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
                Boolean.TRUE.equals(producto.getBajoPedido()),
                producto.getActivo(),
                costo,
                margen,
                margenPct
        );
    }
}
