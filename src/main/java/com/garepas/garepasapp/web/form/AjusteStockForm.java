package com.garepas.garepasapp.web.form;

import com.garepas.garepasapp.domain.command.AjusteStockCommand;
import com.garepas.garepasapp.domain.command.DetalleAjusteStockCommand;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

  









public class AjusteStockForm {

    private List<Long> insumoId = new ArrayList<>();
    private List<BigDecimal> cantidad = new ArrayList<>();

      
    public List<DetalleAjusteStockCommand> filasValidas() {
        List<DetalleAjusteStockCommand> filas = new ArrayList<>();
        for (int i = 0; i < insumoId.size(); i++) {
            Long id = insumoId.get(i);
            BigDecimal c = i < cantidad.size() ? cantidad.get(i) : null;
            if (id != null && c != null && c.compareTo(BigDecimal.ZERO) != 0) {
                filas.add(new DetalleAjusteStockCommand(id, c));
            }
        }
        return filas;
    }

    public AjusteStockCommand toCommand() {
        return new AjusteStockCommand(filasValidas());
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
}
