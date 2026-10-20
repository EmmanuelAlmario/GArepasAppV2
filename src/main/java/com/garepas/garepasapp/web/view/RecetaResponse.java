package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.model.Receta;

import java.math.BigDecimal;
import java.util.List;

public record RecetaResponse(
        Long id,
        String nombre,
        String descripcion,
        List<DetalleRecetaResponse> detalles,
        BigDecimal costoTotal,
        BigDecimal costoUnitario,
        Long insumoProducidoId,
        BigDecimal rendimiento
) {
    public static RecetaResponse desde(Receta receta) {
        List<DetalleRecetaResponse> detalles = receta.getDetalles().stream()
                .map(DetalleRecetaResponse::desde)
                .toList();
        BigDecimal costoTotal = detalles.stream()
                .map(DetalleRecetaResponse::subtotalCosto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal costoUnitario = costoTotal.divide(
                com.garepas.garepasapp.domain.service.CostoService.rendimientoDe(receta),
                6, java.math.RoundingMode.HALF_UP);
        return new RecetaResponse(
                receta.getId(),
                receta.getNombre(),
                receta.getDescripcion(),
                detalles,
                costoTotal,
                costoUnitario,
                receta.getInsumoProducido() != null ? receta.getInsumoProducido().getId() : null,
                receta.getRendimiento()
        );
    }
}
