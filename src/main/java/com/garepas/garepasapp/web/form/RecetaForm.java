package com.garepas.garepasapp.web.form;

import com.garepas.garepasapp.domain.command.DetalleRecetaCommand;
import com.garepas.garepasapp.domain.command.RecetaCommand;
import com.garepas.garepasapp.domain.enums.UnidadMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulario de receta. Los detalles llegan como listas paralelas
 * (insumoId, cantidad, unidadMedida repetidos por fila) para poder agregar
 * filas dinamicamente sin manejar indices.
 *
 * <p>Las cantidades NO se convierten aqui: llegan en la unidad que escribio
 * el usuario y el servicio las lleva a unidad base.
 */
public class RecetaForm {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede superar 50 caracteres")
    private String nombre;

    @Size(max = 150, message = "La descripcion no puede superar 150 caracteres")
    private String descripcion;

    private List<Long> insumoId = new ArrayList<>();
    private List<BigDecimal> cantidad = new ArrayList<>();
    private List<UnidadMedida> unidadMedida = new ArrayList<>();

    /** Opcional: insumo que fabrica la receta (ej.: masa preparada). */
    private Long insumoProducidoId;

    /** Cuantas unidades rinde un lote de la receta. */
    @DecimalMin(value = "0.0001", message = "El rendimiento debe ser mayor a cero")
    private BigDecimal rendimiento;

    /** Filas completas: insumo + cantidad mayor que cero + unidad. */
    public List<DetalleRecetaCommand> filasValidas() {
        List<DetalleRecetaCommand> detalles = new ArrayList<>();
        for (int i = 0; i < insumoId.size(); i++) {
            Long insumo = insumoId.get(i);
            BigDecimal cant = i < cantidad.size() ? cantidad.get(i) : null;
            UnidadMedida unidad = i < unidadMedida.size() ? unidadMedida.get(i) : null;
            if (insumo != null && cant != null && cant.compareTo(BigDecimal.ZERO) > 0 && unidad != null) {
                detalles.add(new DetalleRecetaCommand(insumo, cant, unidad));
            }
        }
        return detalles;
    }

    public RecetaCommand toCommand() {
        return new RecetaCommand(nombre, descripcion, filasValidas(), insumoProducidoId, rendimiento);
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
