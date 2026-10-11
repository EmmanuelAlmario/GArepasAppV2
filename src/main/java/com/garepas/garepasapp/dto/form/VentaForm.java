package com.garepas.garepasapp.dto.form;

import com.garepas.garepasapp.dto.request.DetalleVentaRequest;
import com.garepas.garepasapp.dto.request.VentaRequest;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Formulario del punto de venta. El carrito (JavaScript) envia listas
 * paralelas repetidas: productoId y cantidad.
 */
public class VentaForm {

    @Size(max = 100, message = "El nombre del cliente es demasiado largo")
    private String nombreCliente;

    private List<Long> productoId = new ArrayList<>();
    private List<Integer> cantidad = new ArrayList<>();

    public List<DetalleVentaRequest> detallesValidos() {
        List<DetalleVentaRequest> detalles = new ArrayList<>();
        for (int i = 0; i < productoId.size(); i++) {
            Long producto = productoId.get(i);
            Integer cant = i < cantidad.size() ? cantidad.get(i) : null;
            if (producto != null && cant != null && cant > 0) {
                detalles.add(new DetalleVentaRequest(producto, cant));
            }
        }
        return detalles;
    }

    public VentaRequest toRequest() {
        String cliente = (nombreCliente == null || nombreCliente.isBlank()) ? null : nombreCliente.trim();
        return new VentaRequest(cliente, detallesValidos());
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
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