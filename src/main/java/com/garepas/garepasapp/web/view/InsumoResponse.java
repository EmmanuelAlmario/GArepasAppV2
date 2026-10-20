package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.enums.UnidadMedida;
import com.garepas.garepasapp.domain.model.Insumo;

import java.math.BigDecimal;

  






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

      
    public BigDecimal stockPresentacion() {
        return stockActual;
    }

      
    public BigDecimal stockMinimoPresentacion() {
        return stockMinimo;
    }

      
    public String unidadBaseEtiqueta() {
        return switch (unidadBase()) {
            case GRAMO -> "g";
            case MILILITRO -> "ml";
            default -> "u";
        };
    }

      
    public String unidadEtiqueta() {
        return unidadPresentacion == null ? "" : unidadPresentacion.getEtiqueta();
    }
}
