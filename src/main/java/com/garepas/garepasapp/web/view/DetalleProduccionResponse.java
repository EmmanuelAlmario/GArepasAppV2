package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.enums.UnidadMedida;
import com.garepas.garepasapp.domain.model.DetalleProduccion;

import java.math.BigDecimal;

/**
 * Linea de una produccion vista desde la web.
 *
 * <p>Las cantidades estan en unidad BASE para que el calculo de faltantes
 * (requerido - disponible) sea exactamente el mismo que usa el descuento de
 * stock.
 */
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

    /** Cantidad requerida expresada en la unidad de presentacion del insumo. */
    public BigDecimal cantidadRequeridaPresentacion() {
        return insumoUnidadPresentacion == null
                ? cantidadRequerida
                : insumoUnidadPresentacion.desdeBase(cantidadRequerida);
    }

    /** Stock disponible expresado en la unidad de presentacion del insumo. */
    public BigDecimal stockDisponiblePresentacion() {
        return insumoUnidadPresentacion == null
                ? stockDisponible
                : insumoUnidadPresentacion.desdeBase(stockDisponible);
    }

    /** Etiqueta de la unidad base (g, ml, u). */
    public String unidadEtiqueta() {
        return switch (insumoUnidadBase()) {
            case GRAMO -> "g";
            case MILILITRO -> "ml";
            default -> "u";
        };
    }
}
