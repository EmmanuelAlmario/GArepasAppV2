package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.model.Jornada;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record JornadaResponse(
        Long id,
        LocalDateTime fechaApertura,
        String abiertaPor,
        LocalDateTime fechaCierre,
        Boolean activa,
        long nroVentas,
        BigDecimal totalVentas
) {
    public static JornadaResponse desde(Jornada jornada, long nroVentas, BigDecimal totalVentas) {
        return new JornadaResponse(
                jornada.getId(),
                jornada.getFechaApertura(),
                jornada.getAbiertaPor(),
                jornada.getFechaCierre(),
                jornada.getActiva(),
                nroVentas,
                totalVentas == null ? BigDecimal.ZERO : totalVentas
        );
    }
}
