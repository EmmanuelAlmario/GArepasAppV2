package com.garepas.garepasapp.web.view;

import java.math.BigDecimal;

/**
 * Fila de un ranking (productos más vendidos, gastos por categoría) con el
 * porcentaje ya calculado: el navegador lo usa para el ancho de la barra.
 *
 * <p>El porcentaje se calcula en Java y no en la vista: evitar expresiones
 * SpEL con tipos y conversiones dentro del HTML.
 */
public record RankingFila(
        String nombre,
        BigDecimal monto,
        String nota,
        int porcentaje
) {
}
