package com.garepas.garepasapp.domain.command;

import com.garepas.garepasapp.domain.enums.UnidadMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Intencion de alta o edicion de un insumo.
 *
 * <p><b>Unidades:</b> el stock y el precio llegan en la unidad de PRESENTACION
 * (la que elige el usuario). El servicio es el unico punto que las convierte a
 * unidad base. Antes cada ruta de entrada conviertia por su cuenta y el
 * resultado dependia de si el dato venia del formulario o de la API.
 */
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
        // La validacion declarativa vive en el formulario web; aqui se normaliza.
        if (activo == null) {
            activo = Boolean.TRUE;
        }
    }
}
