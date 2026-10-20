package com.garepas.garepasapp.shared;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

  











@Component
public class Formato {

    private static final Locale COLOMBIA = Locale.of("es", "CO");

      
    public BigDecimal limpiar(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        BigDecimal limpio = valor.stripTrailingZeros();
        return limpio.scale() < 0 ? limpio.setScale(0) : limpio;
    }

      
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

      
    public String cantidad(BigDecimal valor) {
        return num(valor, 2);
    }

      
    public String precio(BigDecimal valor) {
        return num(valor, 4);
    }

      
    public String moneda(BigDecimal valor) {
        return valor == null ? "" : "$" + num(valor, 0);
    }

      
    public BigDecimal pesos(BigDecimal valor) {
        return valor == null ? null : valor.setScale(0, RoundingMode.HALF_UP);
    }

      
    public String porcentaje(BigDecimal valor) {
        return valor == null ? "" : num(valor, 1) + "%";
    }
}