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

/**
 * Punto de venta.
 *
 * <p><b>Reglas del rediseño:</b>
 * <ul>
 *   <li>Las lineas del mismo producto se CONSOLIDAN antes de validar el
 *       stock. Antes se validaba linea por linea contra el stock original, de
 *       modo que dos lineas del mismo producto (5 + 5 con 8 disponibles)
 *       pasaban las dos y el inventario quedaba en -2.</li>
 *   <li>Los productos se bloquean ordenados por id, igual que los insumos:
 *       dos cajas registrando ventas a la vez no se bloquean en orden distinto
 *       y se produce un deadlock.</li>
 *   <li>Una venta no se borra: se ANULA. Se conserva el historico (el
 *       reporte ya emitido no debe cambiar) pero deja de contar en ingresos,
 *       reportes y arqueo de jornada.</li>
 *   <li>El importe se descompone en subtotal + impuestos (segun la
 *       configuracion del negocio) y una venta a CREDITO queda con saldo
 *       pendiente de cobro.</li>
 * </ul>
 */
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

    /** Historial con busqueda y filtro por estado, resuelto en base de datos. */
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

    /** La venta con sus abonos, para el estado de cuenta y el recibo. */
    @Transactional(readOnly = true)
    public Venta buscarEntidad(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", id));
    }

    /** Ventas de un cliente (estado de cuenta). */
    @Transactional(readOnly = true)
    public List<Venta> ventasDeCliente(Long clienteId) {
        List<Venta> ventas = ventaRepository.findByClienteIdAndEstado(clienteId, EstadoVenta.ACTIVA);
        // Los detalles se cargan aparte (mismo patron que listarPaginado): el
        // estado de cuenta los muestra y "open-in-view" esta desactivado, asi
        // que la vista no puede inicializar la coleccion perezosa.
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

    /**
     * Registra una venta. Es atomica: si algo falla, no queda ni stock
     * movido ni venta guardada.
     */
    @Transactional
    public VentaResponse registrar(VentaCommand command) {
        if (command.detalles() == null || command.detalles().isEmpty()) {
            throw new OperacionInvalidaException("La venta debe incluir al menos un detalle");
        }

        // 1) Cliente: puede ser uno de la cartera o un consumidor final.
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

        // 2) Forma de pago: a credito exige cliente y deja saldo pendiente.
        FormaPago formaPago = command.formaPago() == null ? FormaPago.EFECTIVO : command.formaPago();
        if (formaPago.esCredito() && cliente == null) {
            throw new OperacionInvalidaException("Una venta a credito necesita un cliente de la cartera");
        }
        LocalDateTime vencimiento = formaPago.esCredito() && command.fechaVencimiento() != null
                ? command.fechaVencimiento().atStartOfDay()
                : null;

        // 3) Consolidar: el mismo producto puede repetirse en varias lineas.
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

        // 4) Bloquear y validar los productos (stock ya consolidado).
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

        // 5) Cabecera. Se persiste primero para que las producciones automaticas
        //    puedan enlazarse a la venta.
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

        // 6) Detalle y efectos sobre el inventario.
        List<DetalleVenta> detalles = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleVentaCommand detalle : command.detalles()) {
            Producto producto = productos.get(detalle.productoId());

            if (esBajoPedido(producto)) {
                // Producto sin existencias: se elabora al momento. La produccion
                // automatica consume los insumos de la receta y lanza si no
                // alcanzan (la venta entera se revierte).
                produccionService.registrarParaVenta(producto, detalle.cantidad(), venta);
            } else {
                inventarioService.descontarProducto(producto, detalle.cantidad());
            }

            // Precio del servidor (snapshot); nunca el que envia el cliente.
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

        // 7) Importes: subtotal + impuestos segun la configuracion del negocio.
        BigDecimal impuestoPct = configuracionNegocioService.impuestoPorcentaje();
        BigDecimal impuestos = subtotal.multiply(impuestoPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(impuestos);

        venta.setSubtotal(subtotal);
        venta.setImpuestos(impuestos);
        venta.setTotal(total);
        // A credito queda pendiente de cobro; el resto se cobra al contado.
        venta.setSaldo(formaPago.esCredito() ? total : BigDecimal.ZERO);
        venta.setDetalles(detalles);
        return VentaResponse.desde(ventaRepository.save(venta));
    }

    /**
     * Anula una venta: devuelve el stock, revierte las producciones automaticas
     * y marca la venta como ANULADA. El registro se conserva como historia.
     */
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

        // Revierte las producciones automaticas (devuelve insumos y borra su gasto).
        produccionService.revertirDeVenta(venta);

        venta.setEstado(EstadoVenta.ANULADA);
        venta.setSaldo(BigDecimal.ZERO);
        return VentaResponse.desde(ventaRepository.save(venta));
    }

    /** Anula una venta (alias semantico usado por la capa web). */
    @Transactional
    public void eliminar(Long id) {
        anular(id);
    }

    private boolean esBajoPedido(Producto producto) {
        return Boolean.TRUE.equals(producto.getBajoPedido());
    }
}
