package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.model.Producto;

import java.math.BigDecimal;

/**
 * Vista publica de un producto para el menu de clientes.
 *
 * Lista blanca deliberada: NO incluye costos, margen, stock, receta ni
 * insumos. Esos datos son internos del negocio y no deben salir de la API
 * publica aunque el cliente los consulte directamente.
 */
public record MenuPublicoItemResponse(
        Long id,
        String nombre,
        BigDecimal precio,
        Boolean disponible
) {
    public static MenuPublicoItemResponse desde(Producto producto) {
        // Misma regla que el punto de venta: un producto bajo pedido siempre
        // se puede pedir; los demas solo si hay stock terminado.
        boolean bajoPedido = Boolean.TRUE.equals(producto.getBajoPedido());
        boolean conStock = producto.getStockActual() != null && producto.getStockActual() > 0;
        return new MenuPublicoItemResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecioVenta(),
                bajoPedido || conStock);
    }
}
