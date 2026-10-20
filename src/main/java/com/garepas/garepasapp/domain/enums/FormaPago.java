package com.garepas.garepasapp.domain.enums;

  






public enum FormaPago {

    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia"),
    CREDITO("Credito");

    private final String etiqueta;

    FormaPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

      
    public boolean esCredito() {
        return this == CREDITO;
    }
}
