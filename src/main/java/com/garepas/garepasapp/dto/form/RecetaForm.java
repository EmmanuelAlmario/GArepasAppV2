package com.garepas.garepasapp.dto.form;

import com.garepas.garepasapp.dto.request.DetalleRecetaRequest;
import com.garepas.garepasapp.dto.request.RecetaRequest;
import com.garepas.garepasapp.enums.UnidadMedida;
import com.garepas.garepasapp.util.UnidadUtil;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulario de receta. Los detalles se envian como listas paralelas
 * (insumoId, cantidad, unidadMedida repetidos por fila) para permitir
 * agregar filas dinamicamente sin manejar indices.
 */
public class RecetaForm {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede superar 50 caracteres")
    private String nombre;

    @Size(max = 150, message = "La descripción no puede superar 150 caracteres")
    private String descripcion;

    private List<Long> insumoId = new ArrayList<>();
    private List<BigDecimal> cantidad = new ArrayList<>();
    private List<UnidadMedida> unidadMedida = new ArrayList<>();

    /** Opcional: insumo que fabrica la receta (ej.: masa preparada). */
    private Long insumoProducidoId;

    /** Rendimiento del lote en unidad base del insumo producido. */
    @DecimalMin(value = "0.0001", message = "El rendimiento debe ser mayor a cero")
    private BigDecimal rendimiento;

    /** Filas completas, convertidas a unidad base. */
    public List<DetalleRecetaRequest> filasValidas() {
        List<DetalleRecetaRequest> detalles = new ArrayList<>();
        for (int i = 0; i < insumoId.size(); i++) {
            Long insumo = insumoId.get(i);
            BigDecimal cant = i < cantidad.size() ? cantidad.get(i) : null;
            UnidadMedida unidad = i < unidadMedida.size() ? unidadMedida.get(i) : null;
            if (insumo != null && cant != null && cant.compareTo(BigDecimal.ZERO) > 0 && unidad != null) {
                detalles.add(new DetalleRecetaRequest(
                        insumo,
                        UnidadUtil.aBase(cant, unidad),
                        UnidadUtil.base(unidad)));
            }
        }
        return detalles;
    }

    public RecetaRequest toRequest() {
        return new RecetaRequest(nombre, descripcion, filasValidas(), insumoProducidoId, rendimiento);
    }

    public Long getInsumoProducidoId() {
        return insumoProducidoId;
    }

    public void setInsumoProducidoId(Long insumoProducidoId) {
        this.insumoProducidoId = insumoProducidoId;
    }

    public BigDecimal getRendimiento() {
        return rendimiento;
    }

    public void setRendimiento(BigDecimal rendimiento) {
        this.rendimiento = rendimiento;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<Long> getInsumoId() {
        return insumoId;
    }

    public void setInsumoId(List<Long> insumoId) {
        this.insumoId = insumoId;
    }

    public List<BigDecimal> getCantidad() {
        return cantidad;
    }

    public void setCantidad(List<BigDecimal> cantidad) {
        this.cantidad = cantidad;
    }

    public List<UnidadMedida> getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(List<UnidadMedida> unidadMedida) {
        this.unidadMedida = unidadMedida;
    }
}