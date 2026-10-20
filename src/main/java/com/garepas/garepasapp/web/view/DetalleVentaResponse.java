package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.model.DetalleVenta;
import java.math.BigDecimal;

public record DetalleVentaResponse(
        Long id,
        Long productoId,
        String productoNombre,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
    public static DetalleVentaResponse desde(DetalleVenta detalle) {
        return new DetalleVentaResponse(
                detalle.getId(),
                detalle.getProducto().getId(),
                detalle.getProducto().getNombre(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal()
        );
    }
}
