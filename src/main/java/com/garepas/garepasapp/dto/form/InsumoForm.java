package com.garepas.garepasapp.dto.form;

import com.garepas.garepasapp.dto.request.InsumoRequest;
import com.garepas.garepasapp.enums.UnidadMedida;
import com.garepas.garepasapp.util.UnidadUtil;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formulario de insumo: el usuario ingresa stock y precio en la unidad de
 * presentacion elegida (kg, L, cucharada, taza...) y se convierten a la
 * unidad base (gramo, mililitro, unidad) al guardar.
 */
public class InsumoForm {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
    private String nombre;

    @NotBlank(message = "La categoria es obligatoria")
    @Size(max = 30, message = "La categoria no puede superar 30 caracteres")
    private String categoria;

    @NotBlank(message = "La marca es obligatoria")
    @Size(max = 30, message = "La marca no puede superar 30 caracteres")
    private String marca;

    @NotNull(message = "El stock actual es obligatorio")
    @DecimalMin(value = "0.0", message = "El stock no puede ser negativo")
    private BigDecimal stockActual;

    @DecimalMin(value = "0.0", message = "El stock minimo no puede ser negativo")
    private BigDecimal stockMinimo;

    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a cero")
    @Digits(integer = 12, fraction = 0, message = "El precio debe ser un valor en pesos, sin decimales")
    private BigDecimal precioCompra;

    @NotNull(message = "La unidad de medida es obligatoria")
    private UnidadMedida unidadMedida;

    private Boolean activo = Boolean.TRUE;

    public InsumoRequest toRequest() {
        BigDecimal precioPorGramo = precioCompra
                .divide(UnidadUtil.factor(unidadMedida), 6, RoundingMode.HALF_UP);
        return new InsumoRequest(
                nombre,
                categoria,
                marca,
                UnidadUtil.aBase(stockActual, unidadMedida),
                stockMinimo == null ? null : UnidadUtil.aBase(stockMinimo, unidadMedida),
                precioPorGramo,
                UnidadUtil.base(unidadMedida),
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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public BigDecimal getStockActual() {
        return stockActual;
    }

    public void setStockActual(BigDecimal stockActual) {
        this.stockActual = stockActual;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = precioCompra;
    }

    public UnidadMedida getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(UnidadMedida unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}