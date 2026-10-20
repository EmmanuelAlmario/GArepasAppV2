package com.garepas.garepasapp.web.form;

import com.garepas.garepasapp.domain.command.DetalleVentaCommand;
import com.garepas.garepasapp.domain.command.VentaCommand;
import com.garepas.garepasapp.domain.enums.FormaPago;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulario del punto de venta. El carrito (JavaScript) envia listas
 * paralelas repetidas: productoId y cantidad.
 *
 * <p>Ademas del pedido se elige a quien se le vende (cliente de la cartera o
 * consumidor final) y como se paga (de contado o a credito).
 */
public class VentaForm {

    @Size(max = 100, message = "El nombre del cliente es demasiado largo")
    private String nombreCliente;

    /** Cliente de la cartera. null = consumidor final. */
    private Long clienteId;

    /** Forma de pago. */
    private FormaPago formaPago = FormaPago.EFECTIVO;

    /** Fecha limite de pago (solo si es a credito). */
    private LocalDate fechaVencimiento;

    private List<Long> productoId = new ArrayList<>();
    private List<Integer> cantidad = new ArrayList<>();

    public List<DetalleVentaCommand> detallesValidos() {
        List<DetalleVentaCommand> detalles = new ArrayList<>();
        for (int i = 0; i < productoId.size(); i++) {
            Long producto = productoId.get(i);
            Integer cant = i < cantidad.size() ? cantidad.get(i) : null;
            if (producto != null && cant != null && cant > 0) {
                detalles.add(new DetalleVentaCommand(producto, cant));
            }
        }
        return detalles;
    }

    public VentaCommand toCommand() {
        String cliente = (nombreCliente == null || nombreCliente.isBlank()) ? null : nombreCliente.trim();
        return new VentaCommand(cliente, clienteId, formaPago, fechaVencimiento, detallesValidos());
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public FormaPago getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(FormaPago formaPago) {
        this.formaPago = formaPago;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public List<Long> getProductoId() {
        return productoId;
    }

    public void setProductoId(List<Long> productoId) {
        this.productoId = productoId;
    }

    public List<Integer> getCantidad() {
        return cantidad;
    }

    public void setCantidad(List<Integer> cantidad) {
        this.cantidad = cantidad;
    }
}
