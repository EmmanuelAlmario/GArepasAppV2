package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.enums.CategoriaGasto;
import com.garepas.garepasapp.domain.model.Gasto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GastoResponse(
        Long id,
        String descripcion,
        BigDecimal monto,
        CategoriaGasto categoria,
        String categoriaEtiqueta,
        LocalDateTime fecha,
        Long produccionId,
        Boolean automatico
) {
    public static GastoResponse desde(Gasto gasto) {
        CategoriaGasto cat = gasto.getCategoria() != null ? gasto.getCategoria() : CategoriaGasto.OTROS;
        boolean automatico = gasto.esAutomatico();
        return new GastoResponse(
                gasto.getId(),
                gasto.getDescripcion(),
                gasto.getMonto(),
                cat,
                cat.getEtiqueta(),
                gasto.getFecha(),
                automatico ? gasto.getProduccion().getId() : null,
                automatico
        );
    }
}
