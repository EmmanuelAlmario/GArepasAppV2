package com.garepas.garepasapp.dto.form;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulario de ajuste de stock multiple (UC5).
 * Las filas se envian como listas paralelas con el mismo nombre de campo
 * (insumoId repetido y cantidad repetida), lo que permite agregar filas
 * dinamicamente sin manejar indices.
 */
public class AjusteStockForm {

    private List<Long> insumoId = new ArrayList<>();
    private List<BigDecimal> cantidad = new ArrayList<>();

    /** Devuelve solo las filas completas (insumo + cantidad distinta de cero). */
    public List<Fila> filasValidas() {
        List<Fila> filas = new ArrayList<>();
        for (int i = 0; i < insumoId.size(); i++) {
            Long id = insumoId.get(i);
            BigDecimal c = i < cantidad.size() ? cantidad.get(i) : null;
            if (id != null && c != null && c.compareTo(BigDecimal.ZERO) != 0) {
                filas.add(new Fila(id, c));
            }
        }
        return filas;
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

    public record Fila(Long insumoId, BigDecimal cantidad) {
    }
}