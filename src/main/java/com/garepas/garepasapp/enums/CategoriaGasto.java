package com.garepas.garepasapp.enums;

/**
 * Categorias de un gasto operativo del negocio.
 */
public enum CategoriaGasto {
    MATERIA_PRIMA("Materia Prima"),
    SERVICIOS("Servicios"),
    TRANSPORTE("Transporte"),
    EQUIPAMIENTO("Equipamiento"),
    MANTENIMIENTO("Mantenimiento"),
    OTROS("Otros");

    private final String etiqueta;

    CategoriaGasto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}