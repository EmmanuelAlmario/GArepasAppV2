package com.garepas.garepasapp.web.controller;

import com.garepas.garepasapp.web.view.ReporteResumen;
import com.garepas.garepasapp.domain.service.ReporteService;
import com.garepas.garepasapp.web.view.RankingFila;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de rentabilidad del negocio (UC29).
 * Es la pagina inicial del administrador tras iniciar sesion.
 */
@Controller
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping
    public String resumen(
            @RequestParam(required = false) String rango,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {

        LocalDate hoy = LocalDate.now();
        String rangoActivo = (rango != null && !rango.isBlank()) ? rango : (desde != null ? "custom" : "7d");

        LocalDate ini;
        LocalDate fin;
        switch (rangoActivo) {
            case "hoy" -> {
                ini = hoy;
                fin = hoy;
            }
            case "mes" -> {
                ini = hoy.withDayOfMonth(1);
                fin = hoy;
            }
            case "anio" -> {
                ini = hoy.withDayOfYear(1);
                fin = hoy;
            }
            case "custom" -> {
                ini = desde != null ? desde : hoy.minusDays(6);
                fin = hasta != null ? hasta : hoy;
            }
            default -> {
                rangoActivo = "7d";
                ini = hoy.minusDays(6);
                fin = hoy;
            }
        }

        ReporteResumen resumen = reporteService.resumen(ini, fin);
        model.addAttribute("r", resumen);
        model.addAttribute("rangoActivo", rangoActivo);
        model.addAttribute("desdeInput", ini);
        model.addAttribute("hastaInput", fin);

        List<ReporteResumen.SerieDia> serie = resumen.serieIngresosVsGastos();
        model.addAttribute("serieFechas", serie.stream().map(s -> s.fecha().toString()).toList());
        model.addAttribute("serieIngresos", serie.stream().map(ReporteResumen.SerieDia::ingresos).toList());
        model.addAttribute("serieGastos", serie.stream().map(ReporteResumen.SerieDia::gastos).toList());

        // Rankings con el porcentaje ya calculado (para el ancho de las barras).
        model.addAttribute("topProductosView", ranking(
                resumen.topProductos().stream().map(p -> new Fila(p.nombre(), p.ingreso(), p.unidades() + " unidades")).toList(),
                true));
        model.addAttribute("gastosView", ranking(
                resumen.gastosPorCategoria().stream().map(g -> new Fila(g.categoria(), g.monto(), null)).toList(),
                false));

        return "reportes/resumen";
    }

    /** Convierte las filas de un ranking en vistas con su porcentaje relativo al mayor. */
    private List<RankingFila> ranking(List<Fila> filas, boolean conNota) {
        BigDecimal max = filas.stream().map(Fila::monto)
                .max(java.util.Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        return filas.stream().map(f -> new RankingFila(
                f.nombre(),
                f.monto(),
                conNota ? f.nota() : null,
                max.signum() > 0
                        ? f.monto().multiply(BigDecimal.valueOf(100))
                                .divide(max, 0, java.math.RoundingMode.HALF_UP).intValue()
                        : 0)).toList();
    }

    /** Fila intermedia del ranking, antes de convertirla en vista. */
    private record Fila(String nombre, BigDecimal monto, String nota) {
    }
}