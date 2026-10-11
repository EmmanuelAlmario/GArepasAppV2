package com.garepas.garepasapp.enums;

public enum UnidadMedida {
    GRAMO("g"),
    KILOGRAMO("kg"),
    MILILITRO("ml"),
    LITRO("L"),
    UNIDAD("unidad"),
    CUCHARADA("cucharada"),
    TAZA("taza");

    private final String etiqueta;

    UnidadMedida(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Nombre corto y cotidiano para mostrar al usuario. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
