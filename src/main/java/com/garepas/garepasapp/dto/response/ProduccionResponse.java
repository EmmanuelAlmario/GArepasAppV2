package com.garepas.garepasapp.dto.response;

import com.garepas.garepasapp.entity.Produccion;
import com.garepas.garepasapp.enums.EstadoProduccion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProduccionResponse(
        Long id,
        Long productoId,
        String productoNombre,
        Integer cantidad,
        LocalDateTime fecha,
        BigDecimal costoTotal,
        BigDecimal costoUnitario,
        EstadoProduccion estado,
        List<DetalleProduccionResponse> detalles
) {
    public static ProduccionResponse desde(Produccion produccion) {
        BigDecimal costoTotal = produccion.getCostoTotal() != null ? produccion.getCostoTotal() : BigDecimal.ZERO;
        BigDecimal costoUnitario = BigDecimal.ZERO;
        if (produccion.getCantidad() != null && produccion.getCantidad() > 0) {
            costoUnitario = costoTotal.divide(
                    BigDecimal.valueOf(produccion.getCantidad()),
                    4, java.math.RoundingMode.HALF_UP);
        }
        return new ProduccionResponse(
                produccion.getId(),
                produccion.getProducto().getId(),
                produccion.getProducto().getNombre(),
                produccion.getCantidad(),
                produccion.getFecha(),
                costoTotal,
                costoUnitario,
                produccion.getEstado(),
                produccion.getDetalles().stream()
                        .map(DetalleProduccionResponse::desde)
                        .toList()
        );
    }
}
