package com.garepas.garepasapp.domain.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

  

















public enum UnidadMedida {

      
      
    KILOGRAMO("kg", true),
    GRAMO("g", true),
    LITRO("L", false),
    MILILITRO("ml", false),
    UNIDAD("unidad", null);

    private final String etiqueta;
    private final Boolean masa;

    UnidadMedida(String etiqueta, Boolean masa) {
        this.etiqueta = etiqueta;
        this.masa = masa;
    }

      
    public String getEtiqueta() {
        return etiqueta;
    }

      
    public Boolean getMasa() {
        return masa;
    }

    public boolean esMasa() {
        return Boolean.TRUE.equals(masa);
    }

    public boolean esVolumen() {
        return Boolean.FALSE.equals(masa);
    }

      
    public UnidadMedida base() {
        return switch (this) {
            case KILOGRAMO -> GRAMO;
            case LITRO -> MILILITRO;
            default -> this;
        };
    }

      
    public BigDecimal factor() {
        return switch (this) {
            case KILOGRAMO, LITRO -> new BigDecimal("1000");
            default -> BigDecimal.ONE;
        };
    }

      
    public BigDecimal aBase(BigDecimal cantidad) {
        return cantidad.multiply(factor());
    }

      
    public BigDecimal desdeBase(BigDecimal cantidadBase) {
        return cantidadBase.divide(factor(), 4, RoundingMode.HALF_UP);
    }

      
    public BigDecimal precioDesdeBase(BigDecimal precioPorUnidadBase) {
        return precioPorUnidadBase.multiply(factor());
    }

      
    public BigDecimal precioABase(BigDecimal precioPorPresentacion) {
        return precioPorPresentacion.divide(factor(), 6, RoundingMode.HALF_UP);
    }

      



    public boolean compatibleCon(UnidadMedida otra) {
        if (otra == null) {
            return false;
        }
        if (this == UNIDAD || otra == UNIDAD) {
            return this == UNIDAD && otra == UNIDAD;
        }
        return esMasa() == otra.esMasa();
    }
}
