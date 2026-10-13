package com.garepas.garepasapp.dto.form;

import com.garepas.garepasapp.dto.request.GastoRequest;
import com.garepas.garepasapp.enums.CategoriaGasto;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class GastoForm {

    private Long id;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 150, message = "La descripción no puede superar 150 caracteres")
    private String descripcion;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a cero")
    @Digits(integer = 10, fraction = 0, message = "El monto debe ser un valor en pesos, sin decimales")
    private BigDecimal monto;

    @NotNull(message = "La categoría es obligatoria")
    private CategoriaGasto categoria;

    public GastoRequest toRequest() {
        return new GastoRequest(descripcion, monto, categoria);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public CategoriaGasto getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaGasto categoria) {
        this.categoria = categoria;
    }
}