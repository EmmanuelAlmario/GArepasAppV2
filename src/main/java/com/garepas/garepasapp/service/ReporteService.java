package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.response.ReporteResumen;
import com.garepas.garepasapp.entity.DetalleVenta;
import com.garepas.garepasapp.entity.Gasto;
import com.garepas.garepasapp.entity.Venta;
import com.garepas.garepasapp.repository.GastoRepository;
import com.garepas.garepasapp.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula el resumen de rentabilidad (ingresos vs gastos) de un rango de fechas.
 * Si no se indica rango, se usan los ultimos 7 dias hasta hoy.
 */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final VentaRepository ventaRepository;
    private final GastoRepository gastoRepository;

    @Transactional(readOnly = true)
    public ReporteResumen resumen(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        LocalDate fin = (hasta == null || hasta.isAfter(hoy)) ? hoy : hasta;
        LocalDate ini = (desde == null || desde.isAfter(fin)) ? fin.minusDays(6) : desde;

        LocalDateTime comienzo = ini.atStartOfDay();
        LocalDateTime cierre = fin.plusDays(1).atStartOfDay();

        List<Venta> ventas = ventaRepository.findByFechaBetween(comienzo, cierre);
        List<Gasto> gastos = gastoRepository.findByFechaBetween(comienzo, cierre);

        // Serie diaria: se inicializa cada dia del rango en cero.
        Map<LocalDate, BigDecimal> ingresosPorDia = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> gastosPorDia = new LinkedHashMap<>();
        for (LocalDate d = ini; !d.isAfter(fin); d = d.plusDays(1)) {
            ingresosPorDia.put(d, BigDecimal.ZERO);
            gastosPorDia.put(d, BigDecimal.ZERO);
        }

        BigDecimal ingresoTotal = BigDecimal.ZERO;
        Map<Long, ProductoAcumulado> acumuladoPorProducto = new LinkedHashMap<>();

        for (Venta v : ventas) {
            ingresosPorDia.merge(v.getFecha().toLocalDate(), v.getTotal(), BigDecimal::add);
            ingresoTotal = ingresoTotal.add(v.getTotal());

            for (DetalleVenta det : v.getDetalles()) {
                if (det.getProducto() != null) {
                    ProductoAcumulado pa = acumuladoPorProducto.computeIfAbsent(
                            det.getProducto().getId(), k -> new ProductoAcumulado(det.getProducto().getNombre()));
                    pa.unidades += det.getCantidad();
                    pa.ingreso = pa.ingreso.add(det.getSubtotal());
                }
            }
        }

        BigDecimal gastoTotal = BigDecimal.ZERO;
        Map<String, BigDecimal> gastoPorCategoria = new LinkedHashMap<>();
        for (Gasto g : gastos) {
            gastosPorDia.merge(g.getFecha().toLocalDate(), g.getMonto(), BigDecimal::add);
            gastoTotal = gastoTotal.add(g.getMonto());
            String categoria = g.getCategoria() != null ? g.getCategoria().getEtiqueta() : "Otros";
            gastoPorCategoria.merge(categoria, g.getMonto(), BigDecimal::add);
        }

        List<ReporteResumen.SerieDia> serie = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : ingresosPorDia.entrySet()) {
            serie.add(new ReporteResumen.SerieDia(e.getKey(), e.getValue(), gastosPorDia.get(e.getKey())));
        }

        List<ReporteResumen.TopProducto> topProductos = acumuladoPorProducto.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().unidades, a.getValue().unidades))
                .limit(5)
                .map(e -> new ReporteResumen.TopProducto(
                        e.getKey(), e.getValue().nombre, e.getValue().unidades, e.getValue().ingreso))
                .toList();

        List<ReporteResumen.GastoCategoria> categorias = gastoPorCategoria.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> new ReporteResumen.GastoCategoria(e.getKey(), e.getValue()))
                .toList();

        BigDecimal utilidad = ingresoTotal.subtract(gastoTotal);
        BigDecimal margen = pct(utilidad, ingresoTotal);

        return new ReporteResumen(
                ini, fin,
                ingresoTotal, gastoTotal, utilidad, margen,
                serie, topProductos, categorias);
    }

    private static BigDecimal pct(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() <= 0) return BigDecimal.ZERO;
        return parte.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }

    private static class ProductoAcumulado {
        final String nombre;
        int unidades;
        BigDecimal ingreso = BigDecimal.ZERO;

        ProductoAcumulado(String nombre) {
            this.nombre = nombre;
        }
    }
}