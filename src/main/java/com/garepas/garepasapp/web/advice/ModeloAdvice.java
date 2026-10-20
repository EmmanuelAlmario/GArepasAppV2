package com.garepas.garepasapp.web.advice;

import com.garepas.garepasapp.shared.Formato;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

  




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