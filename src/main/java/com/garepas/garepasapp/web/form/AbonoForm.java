package com.garepas.garepasapp.web.form;

import com.garepas.garepasapp.domain.command.AbonoCommand;
import com.garepas.garepasapp.domain.enums.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

  


public class AbonoForm {

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor que cero")
    private BigDecimal monto;

    private MetodoPago metodo = MetodoPago.EFECTIVO;

    @Size(max = 150, message = "La nota no puede superar los 150 caracteres")
    private String nota;

    public AbonoCommand toCommand() {
        return new AbonoCommand(monto, metodo == null ? null : metodo.comoFormaPago(), nota);
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPago metodo) {
        this.metodo = metodo;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }
}
