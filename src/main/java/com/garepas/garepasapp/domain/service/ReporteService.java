package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.enums.CategoriaGasto;
import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.repository.DetalleVentaRepository;
import com.garepas.garepasapp.domain.repository.GastoRepository;
import com.garepas.garepasapp.domain.repository.VentaRepository;
import com.garepas.garepasapp.web.view.ReporteResumen;
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

  














@Service
@RequiredArgsConstructor
public class ReporteService {

    private final VentaRepository ventaRepository;
    private final GastoRepository gastoRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    @Transactional(readOnly = true)
    public ReporteResumen resumen(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        LocalDate fin = (hasta == null || hasta.isAfter(hoy)) ? hoy : hasta;
        LocalDate ini = (desde == null || desde.isAfter(fin)) ? fin.minusDays(6) : desde;

        LocalDateTime inicio = ini.atStartOfDay();
        LocalDateTime cierre = fin.plusDays(1).atStartOfDay();   

        Map<LocalDate, BigDecimal> ingresosPorDia = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> gastosPorDia = new LinkedHashMap<>();
        for (LocalDate d = ini; !d.isAfter(fin); d = d.plusDays(1)) {
            ingresosPorDia.put(d, BigDecimal.ZERO);
            gastosPorDia.put(d, BigDecimal.ZERO);
        }

        BigDecimal ingresoTotal = BigDecimal.ZERO;
        for (Object[] fila : ventaRepository.findImportesPorFecha(EstadoVenta.ACTIVA, inicio, cierre)) {
            LocalDate dia = ((LocalDateTime) fila[0]).toLocalDate();
            BigDecimal total = (BigDecimal) fila[1];
            ingresosPorDia.merge(dia, total, BigDecimal::add);
            ingresoTotal = ingresoTotal.add(total);
        }

        BigDecimal gastoTotal = BigDecimal.ZERO;
        for (Object[] fila : gastoRepository.findImportesPorFecha(inicio, cierre)) {
            LocalDate dia = ((LocalDateTime) fila[0]).toLocalDate();
            BigDecimal monto = (BigDecimal) fila[1];
            gastosPorDia.merge(dia, monto, BigDecimal::add);
            gastoTotal = gastoTotal.add(monto);
        }

        List<ReporteResumen.SerieDia> serie = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> entry : ingresosPorDia.entrySet()) {
            serie.add(new ReporteResumen.SerieDia(
                    entry.getKey(),
                    entry.getValue(),
                    gastosPorDia.getOrDefault(entry.getKey(), BigDecimal.ZERO)));
        }

        List<ReporteResumen.TopProducto> topProductos = new ArrayList<>();
        for (Object[] fila : detalleVentaRepository.topProductosPorUnidades(EstadoVenta.ACTIVA, inicio, cierre)) {
            Long productoId = ((Number) fila[0]).longValue();
            String nombre = (String) fila[1];
            int unidades = ((Number) fila[2]).intValue();
            BigDecimal ingreso = (BigDecimal) fila[3];
            topProductos.add(new ReporteResumen.TopProducto(productoId, nombre, unidades, ingreso));
            if (topProductos.size() == 5) {
                break;
            }
        }

        List<ReporteResumen.GastoCategoria> categorias = new ArrayList<>();
        for (Object[] fila : gastoRepository.sumarPorCategoria(inicio, cierre)) {
            CategoriaGasto categoria = (CategoriaGasto) fila[0];
            BigDecimal monto = (BigDecimal) fila[1];
            categorias.add(new ReporteResumen.GastoCategoria(
                    categoria == null ? "Otros" : categoria.getEtiqueta(), monto));
        }

        BigDecimal utilidad = ingresoTotal.subtract(gastoTotal);
        BigDecimal margen = pct(utilidad, ingresoTotal);

        return new ReporteResumen(ini, fin, ingresoTotal, gastoTotal, utilidad, margen, serie, topProductos, categorias);
    }

    private static BigDecimal pct(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return parte.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }
}
