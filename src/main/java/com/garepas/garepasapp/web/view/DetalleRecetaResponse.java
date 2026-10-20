package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.enums.UnidadMedida;
import com.garepas.garepasapp.domain.model.DetalleReceta;
import com.garepas.garepasapp.domain.service.CostoService;

import java.math.BigDecimal;

  






public record DetalleRecetaResponse(
        Long id,
        Long insumoId,
        String insumoNombre,
        BigDecimal cantidad,
        UnidadMedida unidadBase,
        UnidadMedida insumoUnidadPresentacion,
        BigDecimal precioUnidadBase,
        BigDecimal subtotalCosto
) {
    public static DetalleRecetaResponse desde(DetalleReceta detalle) {
        BigDecimal precio = detalle.getInsumo().getPrecioPorUnidadBase();
        BigDecimal subtotal = CostoService.subtotalDetalle(detalle);
        return new DetalleRecetaResponse(
                detalle.getId(),
                detalle.getInsumo().getId(),
                detalle.getInsumo().getNombre(),
                detalle.getCantidad(),
                detalle.getInsumo().unidadBase(),
                detalle.getInsumo().getUnidadPresentacion(),
                precio,
                subtotal
        );
    }

      
    public BigDecimal cantidadEnPresentacion() {
        return insumoUnidadPresentacion == null
                ? cantidad
                : insumoUnidadPresentacion.desdeBase(cantidad);
    }

      
    public String unidadEtiqueta() {
        return switch (unidadBase()) {
            case GRAMO -> "g";
            case MILILITRO -> "ml";
            default -> "u";
        };
    }
}
