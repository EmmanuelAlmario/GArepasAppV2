package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.repository.DetalleVentaRepository;
import com.garepas.garepasapp.domain.repository.GastoRepository;
import com.garepas.garepasapp.domain.repository.VentaRepository;
import com.garepas.garepasapp.web.view.ReporteResumen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Reporte de rentabilidad: rango semiabierto, serie diaria cuadrada con los
 * totales y agregaciones hechas en base de datos.
 */
class ReporteServiceTest {

    private VentaRepository ventaRepository;
    private GastoRepository gastoRepository;
    private DetalleVentaRepository detalleVentaRepository;
    private ReporteService reporteService;

    @BeforeEach
    void setUp() {
        ventaRepository = mock(VentaRepository.class);
        gastoRepository = mock(GastoRepository.class);
        detalleVentaRepository = mock(DetalleVentaRepository.class);
        reporteService = new ReporteService(ventaRepository, gastoRepository, detalleVentaRepository);
    }

    /**
     * REGRESION del rango: antes se usaba un BETWEEN inclusivo con el limite
     * en la medianoche del dia siguiente. Ese dia fantasma entrava en los
     * totales pero no aparecia en la serie diaria y el reporte no cuadraba
     * consigo mismo. Ahora el rango es semiabierto [desde, hasta).
     */
    @Test
    void elRangoEsSemiabiertoYNoIncluyeLaMedianocheDelDiaSiguiente() {
        when(ventaRepository.findImportesPorFecha(any(), any(), any())).thenReturn(List.of());
        when(gastoRepository.findImportesPorFecha(any(), any())).thenReturn(List.of());
        when(detalleVentaRepository.topProductosPorUnidades(any(), any(), any())).thenReturn(List.of());
        when(gastoRepository.sumarPorCategoria(any(), any())).thenReturn(List.of());

        LocalDate desde = LocalDate.of(2026, 3, 1);
        LocalDate hasta = LocalDate.of(2026, 3, 7);
        reporteService.resumen(desde, hasta);

        ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> cierre = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(ventaRepository).findImportesPorFecha(eq(EstadoVenta.ACTIVA), inicio.capture(), cierre.capture());

        assertEquals(LocalDateTime.of(2026, 3, 1, 0, 0), inicio.getValue());
        assertEquals(LocalDateTime.of(2026, 3, 8, 0, 0), cierre.getValue(),
                "El limite superior es exclusivo: la medianoche del dia siguiente no entra");
    }

    @Test
    void laSerieDiariaSumaElTotalDeIngresosYGastos() {
        when(ventaRepository.findImportesPorFecha(any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{LocalDateTime.of(2026, 3, 1, 10, 0), new BigDecimal("10000")},
                new Object[]{LocalDateTime.of(2026, 3, 2, 10, 0), new BigDecimal("5000")}));
        when(gastoRepository.findImportesPorFecha(any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{LocalDateTime.of(2026, 3, 1, 12, 0), new BigDecimal("2000")}));
        when(detalleVentaRepository.topProductosPorUnidades(any(), any(), any())).thenReturn(List.of());
        when(gastoRepository.sumarPorCategoria(any(), any())).thenReturn(List.of());

        ReporteResumen resumen = reporteService.resumen(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 3));

        assertEquals(0, new BigDecimal("15000").compareTo(resumen.ingresoTotal()));
        assertEquals(0, new BigDecimal("2000").compareTo(resumen.gastoTotal()));
        assertEquals(0, new BigDecimal("13000").compareTo(resumen.utilidadNeta()));
        assertEquals(3, resumen.serieIngresosVsGastos().size());
        assertEquals(0, new BigDecimal("15000").compareTo(
                resumen.serieIngresosVsGastos().stream()
                        .map(ReporteResumen.SerieDia::ingresos)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)),
                "La serie diaria tiene que cuadrar con el total");
    }

    @Test
    void ignoraLasVentasAnuladas() {
        when(ventaRepository.findImportesPorFecha(any(), any(), any())).thenReturn(List.of());
        when(gastoRepository.findImportesPorFecha(any(), any())).thenReturn(List.of());
        when(detalleVentaRepository.topProductosPorUnidades(any(), any(), any())).thenReturn(List.of());
        when(gastoRepository.sumarPorCategoria(any(), any())).thenReturn(List.of());

        reporteService.resumen(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1));

        verify(ventaRepository).findImportesPorFecha(eq(EstadoVenta.ACTIVA), any(), any());
        verify(detalleVentaRepository).topProductosPorUnidades(eq(EstadoVenta.ACTIVA), any(), any());
    }
}
