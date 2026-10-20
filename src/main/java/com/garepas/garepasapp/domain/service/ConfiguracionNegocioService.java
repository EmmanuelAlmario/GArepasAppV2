package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.ConfiguracionNegocioCommand;
import com.garepas.garepasapp.domain.model.ConfiguracionNegocio;
import com.garepas.garepasapp.domain.repository.ConfiguracionNegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuracion del negocio (fila unica).
 *
 * <p>Si no existe todavia (base nueva sin la migracion V3), se crea con los
 * valores por defecto la primera vez que se pide. Asi la aplicacion nunca
 * se queda sin nombre comercial ni impuesto.
 */
@Service
@RequiredArgsConstructor
public class ConfiguracionNegocioService {

    private final ConfiguracionNegocioRepository repository;

    @Transactional
    public ConfiguracionNegocio obtener() {
        return repository.findTopByOrderByIdAsc().orElseGet(() -> repository.save(porDefecto()));
    }

    @Transactional
    public ConfiguracionNegocio guardar(ConfiguracionNegocioCommand command) {
        ConfiguracionNegocio config = repository.findTopByOrderByIdAsc().orElseGet(() -> porDefecto());
        config.setNombreComercial(command.nombreComercial().trim());
        config.setIdentificacionFiscal(limpiar(command.identificacionFiscal()));
        config.setTelefono(limpiar(command.telefono()));
        config.setDireccion(limpiar(command.direccion()));
        config.setImpuestoPorcentaje(
                command.impuestoPorcentaje() == null ? BigDecimal.ZERO : command.impuestoPorcentaje());
        config.setMensajeRecibo(limpiar(command.mensajeRecibo()));
        config.setLogoUrl(limpiar(command.logoUrl()));
        config.setActualizadoEn(LocalDateTime.now());
        return repository.save(config);
    }

    /** Impuesto aplicable a una venta, en porcentaje. */
    @Transactional(readOnly = true)
    public BigDecimal impuestoPorcentaje() {
        return obtener().getImpuestoPorcentaje();
    }

    private ConfiguracionNegocio porDefecto() {
        return ConfiguracionNegocio.builder()
                .nombreComercial("Gordo Arepas")
                .impuestoPorcentaje(BigDecimal.ZERO)
                .mensajeRecibo("Gracias por tu compra. Te esperamos pronto.")
                .logoUrl("/images/gordo_arepas_logo.png")
                .actualizadoEn(LocalDateTime.now())
                .build();
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
