package com.garepas.garepasapp.dto.response;

import com.garepas.garepasapp.entity.Insumo;
import com.garepas.garepasapp.enums.UnidadMedida;
import com.garepas.garepasapp.util.UnidadUtil;
import java.math.BigDecimal;

public record InsumoResponse(
        Long id,
        String nombre,
        String categoria,
        String marca,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        Boolean stockBajo,
        BigDecimal precioPorGramo,
        UnidadMedida unidadMedida,
        Boolean activo
) {
    public static InsumoResponse desde(Insumo insumo) {
        BigDecimal min = insumo.getStockMinimo();
        boolean bajo = min != null && insumo.getStockActual().compareTo(min) < 0;
        return new InsumoResponse(
                insumo.getId(),
                insumo.getNombre(),
                insumo.getCategoria(),
                insumo.getMarca(),
                insumo.getStockActual(),
                min,
                bajo,
                insumo.getPrecioPorGramo(),
                insumo.getUnidadMedida(),
                insumo.getActivo()
        );
    }

    /** Stock en la unidad de presentacion elegida (kg, L, taza...) para mostrar al usuario. */
    public BigDecimal stockPresentacion() {
        return UnidadUtil.desdeBase(stockActual, unidadMedida);
    }

    /** Stock minimo en la unidad de presentacion (null = sin alerta). */
    public BigDecimal stockMinimoPresentacion() {
        return stockMinimo == null ? null : UnidadUtil.desdeBase(stockMinimo, unidadMedida);
    }

    /** Unidad base en la que se almacena el stock (gramo, mililitro o unidad). */
    public UnidadMedida unidadBase() {
        return UnidadUtil.base(unidadMedida);
    }

    /** Etiqueta corta de la unidad base para textos de costo: g, ml o u. */
    public String unidadBaseEtiqueta() {
        return switch (unidadBase()) {
            case GRAMO -> "g";
            case MILILITRO -> "ml";
            default -> "u";
        };
    }
}