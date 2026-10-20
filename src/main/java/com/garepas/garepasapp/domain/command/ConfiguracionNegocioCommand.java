package com.garepas.garepasapp.domain.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Configuracion del negocio: identidad, datos fiscales, impuesto y mensaje
 * del recibo.
 */
public record ConfiguracionNegocioCommand(
        @NotBlank(message = "El nombre comercial es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
        String nombreComercial,
        @Size(max = 40, message = "La identificacion no puede superar los 40 caracteres")
        String identificacionFiscal,
        @Size(max = 30, message = "El telefono no puede superar los 30 caracteres")
        String telefono,
        @Size(max = 150, message = "La direccion no puede superar los 150 caracteres")
        String direccion,
        @NotNull(message = "El impuesto es obligatorio")
        @DecimalMin(value = "0.0", message = "El impuesto no puede ser negativo")
        BigDecimal impuestoPorcentaje,
        @Size(max = 250, message = "El mensaje no puede superar los 250 caracteres")
        String mensajeRecibo,
        @Size(max = 250, message = "La ruta del logo no puede superar los 250 caracteres")
        String logoUrl
) {
}
