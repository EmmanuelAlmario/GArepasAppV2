package com.garepas.garepasapp.dto.response;

import com.garepas.garepasapp.entity.Jornada;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record JornadaResponse(
        Long id,
        LocalDateTime fechaApertura,
        String abiertaPor,
        LocalDateTime fechaCierre,
        Boolean activa,
        int nroVentas,
        BigDecimal totalVentas
) {
    public static JornadaResponse desde(Jornada jornada, int nroVentas, BigDecimal totalVentas) {
        return new JornadaResponse(
                jornada.getId(),
                jornada.getFechaApertura(),
                jornada.getAbiertaPor(),
                jornada.getFechaCierre(),
                jornada.getActiva(),
                nroVentas,
                totalVentas
        );
    }
}