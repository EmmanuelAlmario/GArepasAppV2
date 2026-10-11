package com.garepas.garepasapp.dto.response;

import com.garepas.garepasapp.entity.Gasto;
import com.garepas.garepasapp.enums.CategoriaGasto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GastoResponse(
        Long id,
        String descripcion,
        BigDecimal monto,
        CategoriaGasto categoria,
        String categoriaEtiqueta,
        LocalDateTime fecha,
        Long produccionId
) {
    public static GastoResponse desde(Gasto gasto) {
        CategoriaGasto cat = gasto.getCategoria() != null ? gasto.getCategoria() : CategoriaGasto.OTROS;
        return new GastoResponse(
                gasto.getId(),
                gasto.getDescripcion(),
                gasto.getMonto(),
                cat,
                cat.getEtiqueta(),
                gasto.getFecha(),
                gasto.getProduccionId()
        );
    }
}
