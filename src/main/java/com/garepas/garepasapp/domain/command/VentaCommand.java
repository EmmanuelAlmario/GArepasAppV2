package com.garepas.garepasapp.domain.command;

import com.garepas.garepasapp.domain.enums.FormaPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Venta a registrar.
 *
 * <p>Puede ir a un cliente de la cartera o a un consumidor final (nombre
 * suelto). La forma de pago CREDITO deja la venta pendiente de cobro y exige
 * un cliente y una fecha de vencimiento.
 */
public record VentaCommand(
        @Size(max = 100, message = "El nombre del cliente es demasiado largo")
        String nombreCliente,
        Long clienteId,
        FormaPago formaPago,
        LocalDate fechaVencimiento,
        @NotEmpty(message = "La venta debe incluir al menos un producto")
        @Valid
        List<DetalleVentaCommand> detalles
) {
}
