package com.garepas.garepasapp.dto.form;

import com.garepas.garepasapp.dto.request.ProduccionRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ProduccionForm {

    @NotNull(message = "Selecciona un producto")
    private Long productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    public ProduccionRequest toRequest() {
        return new ProduccionRequest(productoId, cantidad);
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}