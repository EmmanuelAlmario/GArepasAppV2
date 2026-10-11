package com.garepas.garepasapp.enums;

public enum Rol {
    ADMIN("Administrador"),
    VENTAS("Registrador de ventas");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}