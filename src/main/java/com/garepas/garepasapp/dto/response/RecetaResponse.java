package com.garepas.garepasapp.dto.response;

import com.garepas.garepasapp.entity.Receta;
import java.math.BigDecimal;
import java.util.List;

public record RecetaResponse(
        Long id,
        String nombre,
        String descripcion,
        List<DetalleRecetaResponse> detalles,
        BigDecimal costoTotal
) {
    public static RecetaResponse desde(Receta receta) {
        List<DetalleRecetaResponse> detalles = receta.getDetalles().stream()
                .map(DetalleRecetaResponse::desde)
                .toList();
        BigDecimal costoTotal = detalles.stream()
                .map(DetalleRecetaResponse::subtotalCosto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RecetaResponse(
                receta.getId(),
                receta.getNombre(),
                receta.getDescripcion(),
                detalles,
                costoTotal
        );
    }
}
