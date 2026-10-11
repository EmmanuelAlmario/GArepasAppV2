package com.garepas.garepasapp.config;

import com.garepas.garepasapp.util.Formato;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expone atributos comunes a todas las plantillas.
 *
 * - "fmt": formateador de numeros, para usar ${fmt.moneda(precio)}.
 */
@ControllerAdvice
public class ModeloAdvice {

    private final Formato formato;

    public ModeloAdvice(Formato formato) {
        this.formato = formato;
    }

    @ModelAttribute("fmt")
    public Formato formato() {
        return formato;
    }
}