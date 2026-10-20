package com.garepas.garepasapp.domain.enums;

/**
 * Forma de pago de una venta.
 *
 * <p>TREINTA separa el pago de la entrega: una venta puede quedar "a credito"
 * (fiada) y cobrarse mas tarde con abonos. El saldo vive en la venta y cada
 * abono queda registrado en la bitacora.
 */
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

    /** true si la venta se registra como pendiente de cobro. */
    public boolean esCredito() {
        return this == CREDITO;
    }
}
