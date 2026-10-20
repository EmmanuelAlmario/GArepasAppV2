package com.garepas.garepasapp.web.form;

import com.garepas.garepasapp.domain.command.InsumoCommand;
import com.garepas.garepasapp.domain.enums.UnidadMedida;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Formulario de insumo.
 *
 * <p>El usuario escribe el stock y el precio en la unidad de PRESENTACION que
 * elige (kg, L, u). El formulario NO convierte: solo arma el comando. La
 * conversion a unidad base ocurre en el servicio, que es el unico punto del
 * sistema donde se hace. Asi el formulario y la API producen el mismo dato.
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

    /** Stock en la unidad de presentacion elegida. */
    @NotNull(message = "El stock actual es obligatorio")
    @DecimalMin(value = "0.0", message = "El stock no puede ser negativo")
    private BigDecimal stockActual;

    /** Stock minimo en la unidad de presentacion elegida. */
    @DecimalMin(value = "0.0", message = "El stock minimo no puede ser negativo")
    private BigDecimal stockMinimo;

    /** Precio de compra por unidad de presentacion (pesos). */
    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a cero")
    @Digits(integer = 12, fraction = 0, message = "El precio debe ser un valor en pesos, sin decimales")
    private BigDecimal precioCompra;

    /** Unidad de presentacion del insumo (kg, L, u, g, ml). */
    @NotNull(message = "La unidad de medida es obligatoria")
    private UnidadMedida unidadMedida;

    private Boolean activo = Boolean.TRUE;

    public InsumoCommand toCommand() {
        return new InsumoCommand(
                nombre,
                categoria,
                marca,
                stockActual,
                stockMinimo,
                precioCompra,
                unidadMedida,
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
