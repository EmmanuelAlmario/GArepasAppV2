package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.model.Producto;

import java.math.BigDecimal;

  






public record MenuPublicoItemResponse(
        Long id,
        String nombre,
        BigDecimal precio,
        Boolean disponible
) {
    public static MenuPublicoItemResponse desde(Producto producto) {
          
          
        boolean bajoPedido = Boolean.TRUE.equals(producto.getBajoPedido());
        boolean conStock = producto.getStockActual() != null && producto.getStockActual() > 0;
        return new MenuPublicoItemResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecioVenta(),
                bajoPedido || conStock);
    }
}
