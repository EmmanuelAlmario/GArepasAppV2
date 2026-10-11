package com.garepas.garepasapp.dto.response;

import com.garepas.garepasapp.entity.Venta;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public record VentaResponse(
        Long id,
        OffsetDateTime fecha,
        BigDecimal total,
        String nombreCliente,
        Long jornadaId,
        List<DetalleVentaResponse> detalles
) {
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    public static VentaResponse desde(Venta venta) {
        return new VentaResponse(
                venta.getId(),
                venta.getFecha().atZone(ZONA).toOffsetDateTime(),
                venta.getTotal(),
                venta.getNombreCliente(),
                venta.getJornadaId(),
                venta.getDetalles().stream()
                        .map(DetalleVentaResponse::desde)
                        .toList()
        );
    }
}
