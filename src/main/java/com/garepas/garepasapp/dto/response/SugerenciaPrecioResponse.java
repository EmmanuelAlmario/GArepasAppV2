package com.garepas.garepasapp.dto.response;

import java.math.BigDecimal;

public record SugerenciaPrecioResponse(
        Long productoId,
        BigDecimal costoUnitario,
        BigDecimal margenAplicado,
        BigDecimal precioSugerido
) {}
