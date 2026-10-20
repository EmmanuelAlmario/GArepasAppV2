package com.garepas.garepasapp.domain.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Unidad de medida del sistema.
 *
 * <p><b>Regla central del diseño:</b> el sistema almacena SIEMPRE cantidades y
 * precios en <em>unidad base</em> (gramo, mililitro o unidad). La
 * <em>unidad de presentación</em> es la que elige el usuario (kg, L, u) y solo
 * se usa para capturar y mostrar.
 *
 * <p>La conversión ocurre en un unico punto de la aplicacion (la capa de
 * servicio), de modo que el formulario y la API producen exactamente los
 * mismos datos. Antes cada ruta hacia la base conviertia por su cuenta y el
 * resultado dependia de por donde entrara el dato.
 *
 * <p>Se eliminaron {@code CUCHARADA} y {@code TAZA}: convertir "__ cupcakes"
 * a gramos exige la densidad del material (1 taza de harina son ~120 g, no
 * 240 g), y el sistema no modela densidades. Modelar esas equivalencias con
 * densidades de agua producia costos y descuentos de stock equivocados.
 */
public enum UnidadMedida {

    // El orden es el que ve el usuario en los selectores: primero las
    // unidades mas habituales en un negocio de arepas (kg y L).
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

    /** Nombre corto y cotidiano para mostrar al usuario. */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** true si la unidad mide masa (g, kg), false si mide volumen, null si son unidades. */
    public Boolean getMasa() {
        return masa;
    }

    public boolean esMasa() {
        return Boolean.TRUE.equals(masa);
    }

    public boolean esVolumen() {
        return Boolean.FALSE.equals(masa);
    }

    /** Unidad base en la que se almacena todo el sistema para esta unidad. */
    public UnidadMedida base() {
        return switch (this) {
            case KILOGRAMO -> GRAMO;
            case LITRO -> MILILITRO;
            default -> this;
        };
    }

    /** Cuantas unidades base hay en una unidad de presentacion. */
    public BigDecimal factor() {
        return switch (this) {
            case KILOGRAMO, LITRO -> new BigDecimal("1000");
            default -> BigDecimal.ONE;
        };
    }

    /** Convierte una cantidad de presentacion a unidad base. */
    public BigDecimal aBase(BigDecimal cantidad) {
        return cantidad.multiply(factor());
    }

    /** Convierte una cantidad base a la unidad de presentacion. */
    public BigDecimal desdeBase(BigDecimal cantidadBase) {
        return cantidadBase.divide(factor(), 4, RoundingMode.HALF_UP);
    }

    /** Convierte un precio por unidad base a precio por unidad de presentacion. */
    public BigDecimal precioDesdeBase(BigDecimal precioPorUnidadBase) {
        return precioPorUnidadBase.multiply(factor());
    }

    /** Convierte un precio por unidad de presentacion a precio por unidad base. */
    public BigDecimal precioABase(BigDecimal precioPorPresentacion) {
        return precioPorPresentacion.divide(factor(), 6, RoundingMode.HALF_UP);
    }

    /**
     * true si dos unidades pueden describir la misma magnitud. No se puede
     * medir harina en mililitros ni vendas en gramos.
     */
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
