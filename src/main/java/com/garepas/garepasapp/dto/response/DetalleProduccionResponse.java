package com.garepas.garepasapp.dto.response;

import com.garepas.garepasapp.entity.DetalleProduccion;
import com.garepas.garepasapp.enums.UnidadMedida;
import com.garepas.garepasapp.util.UnidadUtil;
import java.math.BigDecimal;

public record DetalleProduccionResponse(
        Long id,
        Long insumoId,
        String insumoNombre,
        BigDecimal cantidadRequerida,
        BigDecimal cantidadUsada,
        BigDecimal stockDisponible,
        UnidadMedida insumoUnidadMedida,
        UnidadMedida insumoUnidadBase,
        BigDecimal precioPorGramoSnapshot,
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
                detalle.getInsumo().getUnidadMedida(),
                UnidadUtil.base(detalle.getInsumo().getUnidadMedida()),
                detalle.getPrecioPorGramoSnapshot(),
                detalle.getCostoLinea(),
                detalle.getSuficiente()
        );
    }
}
