package com.garepas.garepasapp.web.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de rentabilidad del negocio en un rango de fechas.
 */
public record ReporteResumen(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal ingresoTotal,
        BigDecimal gastoTotal,
        BigDecimal utilidadNeta,
        BigDecimal margenPorcentaje,
        List<SerieDia> serieIngresosVsGastos,
        List<TopProducto> topProductos,
        List<GastoCategoria> gastosPorCategoria
) {
    public record SerieDia(LocalDate fecha, BigDecimal ingresos, BigDecimal gastos) {}

    public record TopProducto(Long productoId, String nombre, int unidades, BigDecimal ingreso) {}

    public record GastoCategoria(String categoria, BigDecimal monto) {}
}