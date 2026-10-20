package com.garepas.garepasapp.domain.enums;

/**
 * Estado de una venta.
 *
 * <p>Antes de esta version anular una venta la BORRABA del historico. Eso
 * rompia los reportes ya emitidos y dejaba ventas de la jornada sin rastro.
 * Ahora la venta se marca ANULADA: conserva la historia, pero deja de contar
 * para ingresos, reportes y arqueo de jornada.
 */
public enum EstadoVenta {

    /** Venta vigente: cuenta en ingresos, reportes y arqueo de jornada. */
    ACTIVA,

    /** Venta anulada: se conserva como historia pero no cuenta en los totales. */
    ANULADA;

    public boolean activa() {
        return this == ACTIVA;
    }
}
