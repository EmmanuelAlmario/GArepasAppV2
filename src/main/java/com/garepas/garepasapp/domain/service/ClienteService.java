package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.ClienteCommand;
import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.model.Cliente;
import com.garepas.garepasapp.domain.repository.ClienteRepository;
import com.garepas.garepasapp.domain.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Clientes del negocio: los que fian, los que se les manda a cobrar y los que
 * solo compran de vez en cuando.
 *
 * <p>Un cliente con ventas registradas no se borra: se desactiva. Asi el
 * historico no se rompe.
 */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final VentaRepository ventaRepository;

    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return clienteRepository.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarActivos() {
        return clienteRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }

    @Transactional
    public Cliente crear(ClienteCommand command) {
        if (clienteRepository.existsByNombreIgnoreCase(command.nombre())) {
            throw new RecursoDuplicadoException("Cliente", "nombre", command.nombre());
        }
        Cliente cliente = Cliente.builder()
                .nombre(command.nombre().trim())
                .telefono(limpiar(command.telefono()))
                .direccion(limpiar(command.direccion()))
                .notas(limpiar(command.notas()))
                .activo(command.activo() == null || command.activo())
                .build();
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente actualizar(Long id, ClienteCommand command) {
        Cliente cliente = buscarPorId(id);
        if (!cliente.getNombre().equalsIgnoreCase(command.nombre())
                && clienteRepository.existsByNombreIgnoreCase(command.nombre())) {
            throw new RecursoDuplicadoException("Cliente", "nombre", command.nombre());
        }
        cliente.setNombre(command.nombre().trim());
        cliente.setTelefono(limpiar(command.telefono()));
        cliente.setDireccion(limpiar(command.direccion()));
        cliente.setNotas(limpiar(command.notas()));
        if (command.activo() != null) {
            cliente.setActivo(command.activo());
        }
        return clienteRepository.save(cliente);
    }

    /**
     * Borra el cliente si nunca ha tenido ventas; si ya tiene, lo desactiva
     * (borrarlo dejaria ventas huerfanas).
     */
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscarPorId(id);
        if (ventaRepository.countVentasPorCliente(id) > 0) {
            cliente.setActivo(Boolean.FALSE);
            clienteRepository.save(cliente);
            throw new OperacionInvalidaException(
                    "El cliente tiene ventas registradas: se desactivo en lugar de eliminarlo.");
        }
        clienteRepository.deleteById(id);
    }

    /** Saldo pendiente total de un cliente. */
    @Transactional(readOnly = true)
    public BigDecimal saldoDe(Long clienteId) {
        return ventaRepository.findByClienteIdAndEstado(clienteId, EstadoVenta.ACTIVA).stream()
                .map(v -> v.getSaldo() == null ? BigDecimal.ZERO : v.getSaldo())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
