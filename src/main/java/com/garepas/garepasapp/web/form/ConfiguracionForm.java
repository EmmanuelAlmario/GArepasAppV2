package com.garepas.garepasapp.web.form;

import com.garepas.garepasapp.domain.command.ConfiguracionNegocioCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Formulario de configuracion del negocio: identidad, datos fiscales,
 * impuesto por defecto y mensaje del recibo.
 */
public class ConfiguracionForm {

    @NotBlank(message = "El nombre comercial es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    private String nombreComercial;

    @Size(max = 40, message = "La identificacion no puede superar los 40 caracteres")
    private String identificacionFiscal;

    @Size(max = 30, message = "El telefono no puede superar los 30 caracteres")
    private String telefono;

    @Size(max = 150, message = "La direccion no puede superar los 150 caracteres")
    private String direccion;

    @NotNull(message = "El impuesto es obligatorio")
    @DecimalMin(value = "0.0", message = "El impuesto no puede ser negativo")
    private BigDecimal impuestoPorcentaje;

    @Size(max = 250, message = "El mensaje no puede superar los 250 caracteres")
    private String mensajeRecibo;

    @Size(max = 250, message = "La ruta del logo no puede superar los 250 caracteres")
    private String logoUrl;

    public ConfiguracionNegocioCommand toCommand() {
        return new ConfiguracionNegocioCommand(nombreComercial, identificacionFiscal, telefono,
                direccion, impuestoPorcentaje, mensajeRecibo, logoUrl);
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial;
    }

    public String getIdentificacionFiscal() {
        return identificacionFiscal;
    }

    public void setIdentificacionFiscal(String identificacionFiscal) {
        this.identificacionFiscal = identificacionFiscal;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public BigDecimal getImpuestoPorcentaje() {
        return impuestoPorcentaje;
    }

    public void setImpuestoPorcentaje(BigDecimal impuestoPorcentaje) {
        this.impuestoPorcentaje = impuestoPorcentaje;
    }

    public String getMensajeRecibo() {
        return mensajeRecibo;
    }

    public void setMensajeRecibo(String mensajeRecibo) {
        this.mensajeRecibo = mensajeRecibo;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}
