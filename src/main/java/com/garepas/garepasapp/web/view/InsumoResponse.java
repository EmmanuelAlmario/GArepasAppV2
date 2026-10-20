package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.enums.UnidadMedida;
import com.garepas.garepasapp.domain.model.Insumo;

import java.math.BigDecimal;

/**
 * Vista de un insumo para la capa web.
 *
 * <p>Los valores se muestran en la unidad de PRESENTACION (la que eligió el
 * usuario): stock en kg, precio por kilo. Internamente el sistema trabaja con
 * unidad base; la conversion vive en el dominio.
 */
public record InsumoResponse(
        Long id,
        String nombre,
        String categoria,
        String marca,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        Boolean stockBajo,
        BigDecimal precioPorUnidad,
        UnidadMedida unidadPresentacion,
        UnidadMedida unidadBase,
        Boolean activo
) {
    public static InsumoResponse desde(Insumo insumo) {
        return new InsumoResponse(
                insumo.getId(),
                insumo.getNombre(),
                insumo.getCategoria(),
                insumo.getMarca(),
                insumo.stockEnPresentacion(),
                insumo.stockMinimoEnPresentacion(),
                insumo.stockBajo(),
                insumo.precioEnPresentacion(),
                insumo.getUnidadPresentacion(),
                insumo.unidadBase(),
                insumo.getActivo()
        );
    }

    /** Stock en la unidad de presentacion (lo que ve el usuario). */
    public BigDecimal stockPresentacion() {
        return stockActual;
    }

    /** Stock minimo en la unidad de presentacion (null = sin alerta). */
    public BigDecimal stockMinimoPresentacion() {
        return stockMinimo;
    }

    /** Etiqueta corta de la unidad base para textos de costo: g, ml o u. */
    public String unidadBaseEtiqueta() {
        return switch (unidadBase()) {
            case GRAMO -> "g";
            case MILILITRO -> "ml";
            default -> "u";
        };
    }

    /** Etiqueta de la unidad de presentacion (kg, L, u...). */
    public String unidadEtiqueta() {
        return unidadPresentacion == null ? "" : unidadPresentacion.getEtiqueta();
    }
}
