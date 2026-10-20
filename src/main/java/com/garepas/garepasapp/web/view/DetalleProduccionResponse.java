package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.enums.UnidadMedida;
import com.garepas.garepasapp.domain.model.DetalleProduccion;

import java.math.BigDecimal;

  






public record DetalleProduccionResponse(
        Long id,
        Long insumoId,
        String insumoNombre,
        BigDecimal cantidadRequerida,
        BigDecimal cantidadUsada,
        BigDecimal stockDisponible,
        UnidadMedida insumoUnidadPresentacion,
        UnidadMedida insumoUnidadBase,
        BigDecimal precioUnidadBaseSnapshot,
        BigDecimal costoLinea,
        Boolean suficiente
) {
    public static DetalleProduccionResponse desde(DetalleProduccion detalle) {
        return new DetalleProduccionResponse(
                detalle.getId(),
                detalle.getInsumo().getId(),
                detalle.getInsumo().getNombre(),
                detalle.getCantidadRequerida(),
                detalle.getCantidadUsada(),
                detalle.getInsumo().getStockActual(),
                detalle.getInsumo().getUnidadPresentacion(),
                detalle.getInsumo().unidadBase(),
                detalle.getPrecioUnidadBaseSnapshot(),
                detalle.getCostoLinea(),
                detalle.getSuficiente()
        );
    }

      
    public BigDecimal cantidadRequeridaPresentacion() {
        return insumoUnidadPresentacion == null
                ? cantidadRequerida
                : insumoUnidadPresentacion.desdeBase(cantidadRequerida);
    }

      
    public BigDecimal stockDisponiblePresentacion() {
        return insumoUnidadPresentacion == null
                ? stockDisponible
                : insumoUnidadPresentacion.desdeBase(stockDisponible);
    }

      
    public String unidadEtiqueta() {
        return switch (insumoUnidadBase()) {
            case GRAMO -> "g";
            case MILILITRO -> "ml";
            default -> "u";
        };
    }
}
