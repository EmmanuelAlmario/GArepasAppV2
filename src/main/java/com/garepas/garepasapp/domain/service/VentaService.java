package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.DetalleVentaCommand;
import com.garepas.garepasapp.domain.command.VentaCommand;
import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.enums.FormaPago;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.model.Cliente;
import com.garepas.garepasapp.domain.model.DetalleVenta;
import com.garepas.garepasapp.domain.model.Jornada;
import com.garepas.garepasapp.domain.model.Producto;
import com.garepas.garepasapp.domain.model.Venta;
import com.garepas.garepasapp.domain.repository.ClienteRepository;
import com.garepas.garepasapp.domain.repository.JornadaRepository;
import com.garepas.garepasapp.domain.repository.VentaRepository;
import com.garepas.garepasapp.web.view.VentaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

  



















@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final JornadaRepository jornadaRepository;
    private final ClienteRepository clienteRepository;
    private final ProduccionService produccionService;
    private final InventarioService inventarioService;
    private final ConfiguracionNegocioService configuracionNegocioService;

    @Transactional(readOnly = true)
    public List<VentaResponse> listarTodas() {
        return ventaRepository.findAllByOrderByFechaDesc()
                .stream()
                .map(VentaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<VentaResponse> listarPaginado(Pageable pageable) {
        Page<Venta> page = ventaRepository.findAllBy(pageable);
        List<Long> ids = page.getContent().stream().map(Venta::getId).toList();
        if (!ids.isEmpty()) {
            Map<Long, List<DetalleVenta>> detallesPorVenta =
                    ventaRepository.findDetallesPorVentaIds(ids).stream()
                            .collect(java.util.stream.Collectors.groupingBy(d -> d.getVenta().getId()));
            page.getContent().forEach(v -> v.setDetalles(
                    detallesPorVenta.getOrDefault(v.getId(), List.of())));
        }
        return page.map(VentaResponse::desde);
    }

      
    @Transactional(readOnly = true)
    public Page<VentaResponse> buscar(String texto, EstadoVenta estado, Pageable pageable) {
        String q = texto == null ? "" : texto.trim();
        return ventaRepository.buscar(q, estado, pageable).map(VentaResponse::desde);
    }

    @Transactional(readOnly = true)
    public VentaResponse buscarPorId(Long id) {
        return ventaRepository.findById(id)
                .map(VentaResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", id));
    }

      
    @Transactional(readOnly = true)
    public Venta buscarEntidad(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", id));
    }

      
    @Transactional(readOnly = true)
    public List<Venta> ventasDeCliente(Long clienteId) {
        List<Venta> ventas = ventaRepository.findByClienteIdAndEstado(clienteId, EstadoVenta.ACTIVA);
          
          
          
        List<Long> ids = ventas.stream().map(Venta::getId).toList();
        if (!ids.isEmpty()) {
            Map<Long, List<DetalleVenta>> detallesPorVenta =
                    ventaRepository.findDetallesPorVentaIds(ids).stream()
                            .collect(java.util.stream.Collectors.groupingBy(d -> d.getVenta().getId()));
            ventas.forEach(v -> v.setDetalles(
                    detallesPorVenta.getOrDefault(v.getId(), List.of())));
        }
        return ventas;
    }

      



    @Transactional
    public VentaResponse registrar(VentaCommand command) {
        if (command.detalles() == null || command.detalles().isEmpty()) {
            throw new OperacionInvalidaException("La venta debe incluir al menos un detalle");
        }

          
        Cliente cliente = null;
        String nombreEnVenta = command.nombreCliente();
        if (command.clienteId() != null) {
            cliente = clienteRepository.findById(command.clienteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", command.clienteId()));
            if (!Boolean.TRUE.equals(cliente.getActivo())) {
                throw new OperacionInvalidaException("El cliente '" + cliente.getNombre() + "' esta inactivo");
            }
            nombreEnVenta = cliente.getNombre();
        }

          
        FormaPago formaPago = command.formaPago() == null ? FormaPago.EFECTIVO : command.formaPago();
        if (formaPago.esCredito() && cliente == null) {
            throw new OperacionInvalidaException("Una venta a credito necesita un cliente de la cartera");
        }
        LocalDateTime vencimiento = formaPago.esCredito() && command.fechaVencimiento() != null
                ? command.fechaVencimiento().atStartOfDay()
                : null;

          
        Map<Long, Integer> consolidado = new LinkedHashMap<>();
        for (DetalleVentaCommand detalle : command.detalles()) {
            if (detalle.productoId() == null) {
                throw new OperacionInvalidaException("La venta debe incluir al menos un producto");
            }
            if (detalle.cantidad() == null || detalle.cantidad() <= 0) {
                throw new OperacionInvalidaException("La cantidad debe ser mayor que cero");
            }
            consolidado.merge(detalle.productoId(), detalle.cantidad(), Integer::sum);
        }

          
        Map<Long, Producto> productos = inventarioService.bloquearProductos(consolidado.keySet());
        for (Map.Entry<Long, Integer> entry : consolidado.entrySet()) {
            Producto producto = productos.get(entry.getKey());
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new OperacionInvalidaException(
                        "El producto '" + producto.getNombre() + "' no esta activo");
            }
            if (!esBajoPedido(producto) && producto.getStockActual() < entry.getValue()) {
                throw new OperacionInvalidaException(
                        "Stock insuficiente para el producto '" + producto.getNombre() + "'. Disponible: "
                                + producto.getStockActual() + ", solicitado: " + entry.getValue());
            }
        }

          
          
        Jornada jornada = jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc().orElse(null);
        Venta venta = ventaRepository.save(Venta.builder()
                .fecha(LocalDateTime.now())
                .subtotal(BigDecimal.ZERO)
                .impuestos(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .saldo(BigDecimal.ZERO)
                .formaPago(formaPago)
                .nombreCliente(nombreEnVenta)
                .cliente(cliente)
                .fechaVencimiento(vencimiento)
                .jornada(jornada)
                .estado(EstadoVenta.ACTIVA)
                .build());

          
        List<DetalleVenta> detalles = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleVentaCommand detalle : command.detalles()) {
            Producto producto = productos.get(detalle.productoId());

            if (esBajoPedido(producto)) {
                  
                  
                  
                produccionService.registrarParaVenta(producto, detalle.cantidad(), venta);
            } else {
                inventarioService.descontarProducto(producto, detalle.cantidad());
            }

              
            BigDecimal precioUnitario = producto.getPrecioVenta();
            BigDecimal subtotalLinea = precioUnitario.multiply(BigDecimal.valueOf(detalle.cantidad()));

            detalles.add(DetalleVenta.builder()
                    .venta(venta)
                    .producto(producto)
                    .cantidad(detalle.cantidad())
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotalLinea)
                    .build());
            subtotal = subtotal.add(subtotalLinea);
        }

          
        BigDecimal impuestoPct = configuracionNegocioService.impuestoPorcentaje();
        BigDecimal impuestos = subtotal.multiply(impuestoPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(impuestos);

        venta.setSubtotal(subtotal);
        venta.setImpuestos(impuestos);
        venta.setTotal(total);
          
        venta.setSaldo(formaPago.esCredito() ? total : BigDecimal.ZERO);
        venta.setDetalles(detalles);
        return VentaResponse.desde(ventaRepository.save(venta));
    }

      



    @Transactional
    public VentaResponse anular(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", id));

        if (!venta.estaActiva()) {
            throw new OperacionInvalidaException("La venta #" + id + " ya esta anulada");
        }
        if (venta.getSaldo() != null && venta.getSaldo().signum() > 0) {
            throw new OperacionInvalidaException(
                    "La venta #" + id + " esta pendiente de cobro: primero registra la devolucion del dinero.");
        }

        Map<Long, Producto> productos = inventarioService.bloquearProductos(
                venta.getDetalles().stream().map(d -> d.getProducto().getId()).toList());
        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = productos.get(detalle.getProducto().getId());
            if (!esBajoPedido(producto)) {
                inventarioService.reponerProducto(producto, detalle.getCantidad());
            }
        }

          
        produccionService.revertirDeVenta(venta);

        venta.setEstado(EstadoVenta.ANULADA);
        venta.setSaldo(BigDecimal.ZERO);
        return VentaResponse.desde(ventaRepository.save(venta));
    }

      
    @Transactional
    public void eliminar(Long id) {
        anular(id);
    }

    private boolean esBajoPedido(Producto producto) {
        return Boolean.TRUE.equals(producto.getBajoPedido());
    }
}
