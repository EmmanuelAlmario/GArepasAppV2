package com.garepas.garepasapp.util;

import com.garepas.garepasapp.enums.UnidadMedida;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Conversiones entre unidades de presentacion (kilogramo, litro, cucharada,
 * taza) y las unidades base del sistema (gramo, mililitro, unidad).
 *
 * Los insumos se almacenan en unidad base: precioPorGramo y stockActual
 * siempre estan expresados en gramo, mililitro o unidad.
 */
public final class UnidadUtil {

    private UnidadUtil() {
    }

    /** Unidad base en la que se almacena un insumo segun su unidad de presentacion. */
    public static UnidadMedida base(UnidadMedida unidad) {
        return switch (unidad) {
            case KILOGRAMO, CUCHARADA, TAZA -> UnidadMedida.GRAMO;
            case LITRO -> UnidadMedida.MILILITRO;
            default -> unidad;
        };
    }

    /** Cuantas unidades base hay en una unidad de presentacion. */
    public static BigDecimal factor(UnidadMedida unidad) {
        return switch (unidad) {
            case KILOGRAMO, LITRO -> new BigDecimal("1000");
            case CUCHARADA -> new BigDecimal("15");
            case TAZA -> new BigDecimal("240");
            default -> BigDecimal.ONE;
        };
    }

    /** Convierte una cantidad de presentacion a unidad base. */
    public static BigDecimal aBase(BigDecimal cantidad, UnidadMedida unidad) {
        return cantidad.multiply(factor(unidad));
    }

    /** Convierte una cantidad base a la unidad de presentacion indicada. */
    public static BigDecimal desdeBase(BigDecimal cantidadBase, UnidadMedida unidad) {
        return cantidadBase.divide(factor(unidad), 4, RoundingMode.HALF_UP);
    }
}