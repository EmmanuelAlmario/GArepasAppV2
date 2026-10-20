package com.garepas.garepasapp.shared;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formato de numeros para formularios y vistas (convencion de Colombia:
 * separador de miles "." y separador decimal ",").
 *
 * Reglas:
 *  - Se eliminan los ceros decimales sobrantes (2000.0000 -> 2.000, 6.000000 -> 6).
 *  - Cada tipo de dato limita sus decimales al maximo util:
 *      cantidad 2 · precio 4 · moneda 2 · porcentaje 1.
 *
 * En las plantillas se accede como {@code ${fmt.moneda(x)}} (ver ModeloAdvice).
 * Los formularios HTML siguen usando punto decimal (requisito de los input number).
 */
@Component
public class Formato {

    private static final Locale COLOMBIA = Locale.of("es", "CO");

    /** Deja un BigDecimal sin ceros decimales sobrantes (para mostrarlo en formularios). */
    public BigDecimal limpiar(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        BigDecimal limpio = valor.stripTrailingZeros();
        return limpio.scale() < 0 ? limpio.setScale(0) : limpio;
    }

    /** Numero con maximo {@code maxDecimales} decimales, sin ceros sobrantes, formato es-CO. */
    public String num(BigDecimal valor, int maxDecimales) {
        if (valor == null) {
            return "";
        }
        BigDecimal limpio = limpiar(valor.setScale(maxDecimales, RoundingMode.HALF_UP));
        DecimalFormat formato = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(COLOMBIA));
        formato.setMinimumFractionDigits(0);
        formato.setMaximumFractionDigits(maxDecimales);
        return formato.format(limpio);
    }

    /** Cantidad de inventario (maximo 2 decimales). */
    public String cantidad(BigDecimal valor) {
        return num(valor, 2);
    }

    /** Precio unitario por unidad base (hasta 4 decimales: 0,0125). */
    public String precio(BigDecimal valor) {
        return num(valor, 4);
    }

    /** Monto de dinero en pesos enteros (sin centavos, con simbolo $). */
    public String moneda(BigDecimal valor) {
        return valor == null ? "" : "$" + num(valor, 0);
    }

    /** Redondea un valor monetario a pesos enteros (para precargar formularios). */
    public BigDecimal pesos(BigDecimal valor) {
        return valor == null ? null : valor.setScale(0, RoundingMode.HALF_UP);
    }

    /** Porcentaje (1 decimal). */
    public String porcentaje(BigDecimal valor) {
        return valor == null ? "" : num(valor, 1) + "%";
    }
}