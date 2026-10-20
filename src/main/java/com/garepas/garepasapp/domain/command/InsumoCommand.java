package com.garepas.garepasapp.domain.command;

import com.garepas.garepasapp.domain.enums.UnidadMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

  







public record InsumoCommand(
        String nombre,
        String categoria,
        String marca,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        BigDecimal precioPorPresentacion,
        UnidadMedida unidadPresentacion,
        Boolean activo
) {

    public InsumoCommand {
          
        if (activo == null) {
            activo = Boolean.TRUE;
        }
    }
}
