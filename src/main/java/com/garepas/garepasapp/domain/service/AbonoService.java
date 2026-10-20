package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.AbonoCommand;
import com.garepas.garepasapp.domain.enums.FormaPago;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.model.Abono;
import com.garepas.garepasapp.domain.model.Venta;
import com.garepas.garepasapp.domain.repository.AbonoRepository;
import com.garepas.garepasapp.domain.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cobros de las ventas a credito.
 *
 * <p>Cada abono baja el saldo de la venta. Cuando el saldo llega a cero, la
 * venta queda pagada y deja de contar en la cartera.
 */
@Service
@RequiredArgsConstructor
public class AbonoService {

    private final AbonoRepository abonoRepository;
    private final VentaRepository ventaRepository;

    @Transactional(readOnly = true)
    public List<Abono> listarPorVenta(Long ventaId) {
        return abonoRepository.findByVentaIdOrderByFechaDesc(ventaId);
    }

    /** Todos los cobros recibidos de un cliente. */
    @Transactional(readOnly = true)
    public List<Abono> listarPorCliente(Long clienteId) {
        return abonoRepository.findByClienteId(clienteId);
    }

    /**
     * Registra un abono. No se puede abonar mas de lo pendiente ni a una venta
     * ya pagada.
     */
    @Transactional
    public Abono registrar(Long ventaId, AbonoCommand command) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", ventaId));

        if (!venta.estaActiva()) {
            throw new OperacionInvalidaException("No se puede abonar a una venta anulada");
        }
        BigDecimal saldo = venta.getSaldo() == null ? BigDecimal.ZERO : venta.getSaldo();
        if (saldo.signum() <= 0) {
            throw new OperacionInvalidaException("La venta #" + ventaId + " no tiene saldo pendiente");
        }
        if (command.monto() == null || command.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OperacionInvalidaException("El monto del abono debe ser mayor que cero");
        }
        if (command.monto().compareTo(saldo) > 0) {
            throw new OperacionInvalidaException(
                    "El abono no puede ser mayor al saldo pendiente (" + saldo + ")");
        }

        Abono abono = Abono.builder()
                .venta(venta)
                .monto(command.monto())
                .fecha(LocalDateTime.now())
                .metodo(command.metodo() == null ? FormaPago.EFECTIVO : command.metodo())
                .nota(command.nota() == null || command.nota().isBlank() ? null : command.nota().trim())
                .build();
        Abono guardado = abonoRepository.save(abono);

        venta.setSaldo(saldo.subtract(command.monto()));
        ventaRepository.save(venta);
        return guardado;
    }
}
