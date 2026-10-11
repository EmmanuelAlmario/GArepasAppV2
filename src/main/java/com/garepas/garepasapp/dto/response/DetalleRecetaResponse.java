package com.garepas.garepasapp.dto.response;

import com.garepas.garepasapp.entity.DetalleReceta;
import com.garepas.garepasapp.enums.UnidadMedida;
import com.garepas.garepasapp.service.CostoService;
import java.math.BigDecimal;

public record DetalleRecetaResponse(
        Long id,
        Long insumoId,
        String insumoNombre,
        BigDecimal cantidad,
        UnidadMedida unidadMedida,
        BigDecimal precioPorGramo,
        BigDecimal subtotalCosto
) {
    public static DetalleRecetaResponse desde(DetalleReceta detalle) {
        BigDecimal precio = detalle.getInsumo().getPrecioPorGramo();
        BigDecimal subtotal = CostoService.subtotalDetalle(detalle);
        return new DetalleRecetaResponse(
                detalle.getId(),
                detalle.getInsumo().getId(),
                detalle.getInsumo().getNombre(),
                detalle.getCantidad(),
                detalle.getUnidadMedida(),
                precio,
                subtotal
        );
    }
}
