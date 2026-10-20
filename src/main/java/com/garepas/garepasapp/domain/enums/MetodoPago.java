package com.garepas.garepasapp.domain.enums;

/**
 * Metodos con los que se puede recibir un cobro.
 *
 * <p>Es un catalogo propio (y no el de la forma de pago de la venta) porque
 * una venta a credito siempre nasce como CREDITO, pero el dinero puede
 * entrar en efectivo, por tarjeta o por transferencia.
 */
public enum MetodoPago {

    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia");

    private final String etiqueta;

    MetodoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Convierte el metodo de pago al valor que guarda el abono. */
    public FormaPago comoFormaPago() {
        return FormaPago.valueOf(name());
    }
}
