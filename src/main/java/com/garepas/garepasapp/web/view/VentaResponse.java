package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.enums.FormaPago;
import com.garepas.garepasapp.domain.model.Venta;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Vista de una venta.
 *
 * <p>Incluye el desglose de importes (subtotal, impuestos, total) y el saldo
 * pendiente, para que el historial y el recibo muestren lo mismo que se cobro.
 */
public record VentaResponse(
        Long id,
        OffsetDateTime fecha,
        BigDecimal subtotal,
        BigDecimal impuestos,
        BigDecimal total,
        BigDecimal saldo,
        FormaPago formaPago,
        String formaPagoEtiqueta,
        Long clienteId,
        String nombreCliente,
        Long jornadaId,
        java.time.LocalDateTime fechaVencimiento,
        EstadoVenta estado,
        List<DetalleVentaResponse> detalles
) {
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    public VentaResponse {
        formaPagoEtiqueta = formaPago == null ? FormaPago.EFECTIVO.getEtiqueta() : formaPago.getEtiqueta();
    }

    public static VentaResponse desde(Venta venta) {
        return new VentaResponse(
                venta.getId(),
                venta.getFecha().atZone(ZONA).toOffsetDateTime(),
                venta.getSubtotal(),
                venta.getImpuestos(),
                venta.getTotal(),
                venta.getSaldo(),
                venta.getFormaPago(),
                venta.getFormaPago() == null ? FormaPago.EFECTIVO.getEtiqueta() : venta.getFormaPago().getEtiqueta(),
                venta.getCliente() == null ? null : venta.getCliente().getId(),
                venta.getNombreCliente(),
                venta.getJornada() == null ? null : venta.getJornada().getId(),
                venta.getFechaVencimiento(),
                venta.getEstado() == null ? EstadoVenta.ACTIVA : venta.getEstado(),
                venta.getDetalles().stream()
                        .map(DetalleVentaResponse::desde)
                        .toList()
        );
    }

    /** true si la venta sigue vigente (las anuladas no cuentan). */
    public boolean activa() {
        return estado == null || estado.activa();
    }

    /** true si quedo pendiente de cobro. */
    public boolean pendiente() {
        return saldo != null && saldo.signum() > 0;
    }
}
