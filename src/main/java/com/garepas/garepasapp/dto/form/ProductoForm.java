package com.garepas.garepasapp.dto.form;

import com.garepas.garepasapp.dto.request.ProductoRequest;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class ProductoForm {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede superar 50 caracteres")
    private String nombre;

    @NotNull(message = "El stock actual es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stockActual;

    @Min(value = 0, message = "El stock minimo no puede ser negativo")
    private Integer stockMinimo;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a cero")
    @Digits(integer = 10, fraction = 0, message = "El precio debe ser un valor en pesos, sin decimales")
    private BigDecimal precioVenta;

    private Long recetaId;

    private Boolean activo = Boolean.TRUE;

    public ProductoRequest toRequest() {
        return new ProductoRequest(
                nombre,
                stockActual,
                stockMinimo,
                precioVenta,
                recetaId,
                activo == null ? Boolean.TRUE : activo);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getStockActual() {
        return stockActual;
    }

    public void setStockActual(Integer stockActual) {
        this.stockActual = stockActual;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public Long getRecetaId() {
        return recetaId;
    }

    public void setRecetaId(Long recetaId) {
        this.recetaId = recetaId;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}