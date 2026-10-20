package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.ProduccionCommand;
import com.garepas.garepasapp.domain.enums.CategoriaGasto;
import com.garepas.garepasapp.domain.enums.EstadoProduccion;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.model.DetalleProduccion;
import com.garepas.garepasapp.domain.model.DetalleReceta;
import com.garepas.garepasapp.domain.model.Gasto;
import com.garepas.garepasapp.domain.model.Insumo;
import com.garepas.garepasapp.domain.model.Produccion;
import com.garepas.garepasapp.domain.model.Producto;
import com.garepas.garepasapp.domain.model.Venta;
import com.garepas.garepasapp.domain.repository.GastoRepository;
import com.garepas.garepasapp.domain.repository.ProduccionRepository;
import com.garepas.garepasapp.domain.repository.ProductoRepository;
import com.garepas.garepasapp.web.view.ProduccionResponse;
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
import java.util.stream.Collectors;

/**
 * Produccion: transforma insumos en producto terminado.
 *
 * <p><b>Reglas del rediseño:</b>
 * <ul>
 *   <li>Los ingredientes de una receta se CONSOLIDAN por insumo antes de
 *       calcular. Antes la suficiencia se evaluaba fila por fila: un insumo
 *       repetido en dos filas (60 g + 60 g con 100 g de stock) pasaba las dos
 *       como suficiente y el descuento dejaba el stock en -20.</li>
 *   <li>Los insumos se bloquean ordenados por id (ver InventarioService).</li>
 *   <li>El precio de los insumos queda CONGELADO al crear la produccion. Al
 *       reverificar una produccion pendiente no se recalcula con el precio
 *       actual: el snapshot es justamente para eso.</li>
 *   <li>Descontar y devolver usan la misma cantidad (la usada), de modo que
 *       revertir una produccion devuelve exactamente lo que se desconto.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ProduccionService {

    private final ProduccionRepository produccionRepository;
    private final ProductoRepository productoRepository;
    private final GastoRepository gastoRepository;
    private final InventarioService inventarioService;

    @Transactional(readOnly = true)
    public List<ProduccionResponse> listarTodas() {
        return produccionRepository.findAllByOrderByFechaDesc()
                .stream()
                .map(ProduccionResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProduccionResponse> listarPaginado(Pageable pageable) {
        return produccionRepository.findAllBy(pageable).map(ProduccionResponse::desde);
    }

    @Transactional(readOnly = true)
    public ProduccionResponse buscarPorId(Long id) {
        return produccionRepository.findById(id)
                .map(ProduccionResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Produccion", id));
    }

    /**
     * Produccion manual. Si faltan insumos queda PENDIENTE (no se descuenta
     * stock ni se genera gasto) para completarla mas tarde.
     */
    @Transactional
    public ProduccionResponse registrar(ProduccionCommand command) {
        if (command.cantidad() == null || command.cantidad() <= 0) {
            throw new OperacionInvalidaException("La cantidad debe ser mayor que cero");
        }
        Producto producto = productoRepository.findById(command.productoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", command.productoId()));

        if (producto.getReceta() == null) {
            throw new OperacionInvalidaException(
                    "El producto '" + producto.getNombre() + "' no tiene una receta asignada");
        }
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new OperacionInvalidaException(
                    "El producto '" + producto.getNombre() + "' no esta activo");
        }

        return ProduccionResponse.desde(crearYAplicar(producto, command.cantidad(), null, false));
    }

    /**
     * Produccion automatica generada al vender un producto bajo pedido.
     * Comparte el mismo pipeline de costo y descuento que la produccion manual
     * (una sola fuente de verdad para el consumo de insumos), pero exige
     * insumos suficientes: una venta no puede quedar PENDIENTE.
     */
    @Transactional
    public Produccion registrarParaVenta(Producto producto, Integer cantidad, Venta venta) {
        if (cantidad == null || cantidad <= 0) {
            throw new OperacionInvalidaException("La cantidad debe ser mayor que cero");
        }
        if (!Boolean.TRUE.equals(producto.getBajoPedido()) || producto.getReceta() == null) {
            throw new OperacionInvalidaException(
                    "El producto '" + producto.getNombre() + "' no es un producto bajo pedido con receta");
        }
        return crearYAplicar(producto, cantidad, venta, true);
    }

    /**
     * Revierte las producciones automaticas de una venta anulada: devuelve los
     * insumos y elimina el gasto de materia prima. No devuelve stock de
     * producto porque la produccion automatica nunca lo sumo.
     */
    @Transactional
    public void revertirDeVenta(Venta venta) {
        for (Produccion produccion : produccionRepository.findByVentaId(venta.getId())) {
            if (produccion.getEstado() == EstadoProduccion.COMPLETADA) {
                revertirCompletada(produccion, false);
            }
            produccionRepository.deleteById(produccion.getId());
        }
    }

    private Produccion crearYAplicar(Producto producto, Integer cantidad, Venta venta,
                                     boolean exigirSuficiencia) {
        Produccion produccion = Produccion.builder()
                .producto(producto)
                .cantidad(cantidad)
                .fecha(LocalDateTime.now())
                .costoTotal(BigDecimal.ZERO)
                .estado(EstadoProduccion.PENDIENTE)
                .venta(venta)
                .build();

        List<DetalleProduccion> detalles = construirDetalles(produccion, producto, cantidad, null);
        BigDecimal costoTotal = detalles.stream()
                .map(DetalleProduccion::getCostoLinea)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        produccion.setDetalles(detalles);
        produccion.setCostoTotal(costoTotal);

        boolean todosSuficientes = detalles.stream().allMatch(d -> Boolean.TRUE.equals(d.getSuficiente()));
        if (!todosSuficientes && exigirSuficiencia) {
            throw new OperacionInvalidaException(mensajeInsumosInsuficientes(producto, detalles));
        }

        Produccion guardada = produccionRepository.save(produccion);

        if (todosSuficientes) {
            aplicarProduccion(guardada);
            guardada.setEstado(EstadoProduccion.COMPLETADA);
            guardada = produccionRepository.save(guardada);
        }
        return guardada;
    }

    private String mensajeInsumosInsuficientes(Producto producto, List<DetalleProduccion> detalles) {
        String faltantes = detalles.stream()
                .filter(d -> !Boolean.TRUE.equals(d.getSuficiente()))
                .map(d -> d.getInsumo().getNombre())
                .distinct()
                .collect(Collectors.joining(", "));
        return "Insumos insuficientes para '" + producto.getNombre() + "'. Faltan: " + faltantes
                + ". Reabastece el inventario antes de vender.";
    }

    /**
     * Reverifica el stock de insumos de una produccion PENDIENTE. Si alcanza,
     * descuenta stock, genera el gasto y marca la produccion COMPLETADA.
     *
     * <p>Los precios ya congelados en el snapshot NO se recalculan: la
     * produccion conserva el costo con el que fue creada.
     */
    @Transactional
    public ProduccionResponse verificarYCompletar(Long id) {
        Produccion produccion = produccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Produccion", id));

        if (produccion.getEstado() == EstadoProduccion.COMPLETADA) {
            return ProduccionResponse.desde(produccion);
        }

        List<DetalleProduccion> detalles = construirDetalles(
                produccion, produccion.getProducto(), produccion.getCantidad(), produccion);
        BigDecimal costoTotal = detalles.stream()
                .map(DetalleProduccion::getCostoLinea)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        produccion.getDetalles().clear();
        produccion.getDetalles().addAll(detalles);
        produccion.setCostoTotal(costoTotal);

        boolean todosSuficientes = detalles.stream().allMatch(d -> Boolean.TRUE.equals(d.getSuficiente()));
        if (todosSuficientes) {
            aplicarProduccion(produccion);
            produccion.setEstado(EstadoProduccion.COMPLETADA);
        } else {
            produccion.setEstado(EstadoProduccion.PENDIENTE);
        }

        return ProduccionResponse.desde(produccionRepository.save(produccion));
    }

    /**
     * Construye los detalles de la produccion: consolida los ingredientes de
     * la receta por insumo, calcula la cantidad requerida, la suficiencia con
     * el stock actual y el costo con el precio congelado (o el actual si la
     * produccion es nueva).
     */
    private List<DetalleProduccion> construirDetalles(Produccion produccion, Producto producto,
                                                      Integer cantidad, Produccion previa) {
        // Consolidar ingredientes por insumo (una receta puede repetir un insumo).
        Map<Long, BigDecimal> requeridosPorInsumo = new LinkedHashMap<>();
        for (DetalleReceta detalleReceta : producto.getReceta().getDetalles()) {
            requeridosPorInsumo.merge(
                    detalleReceta.getInsumo().getId(),
                    detalleReceta.getCantidad().multiply(BigDecimal.valueOf(cantidad)),
                    BigDecimal::add);
        }

        Map<Long, Insumo> insumos = inventarioService.bloquearInsumos(requeridosPorInsumo.keySet());

        // Precios congelados de una produccion previa (si existe).
        Map<Long, BigDecimal> preciosCongelados = new LinkedHashMap<>();
        if (previa != null) {
            for (DetalleProduccion detalle : previa.getDetalles()) {
                preciosCongelados.put(detalle.getInsumo().getId(), detalle.getPrecioUnidadBaseSnapshot());
            }
        }

        List<DetalleProduccion> detalles = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : requeridosPorInsumo.entrySet()) {
            Insumo insumo = insumos.get(entry.getKey());
            BigDecimal cantidadRequerida = entry.getValue();
            boolean suficiente = insumo.getStockActual().compareTo(cantidadRequerida) >= 0;

            BigDecimal precioSnapshot = preciosCongelados.getOrDefault(
                    entry.getKey(), insumo.getPrecioPorUnidadBase());
            BigDecimal costoLinea = cantidadRequerida.multiply(precioSnapshot);

            detalles.add(DetalleProduccion.builder()
                    .produccion(produccion)
                    .insumo(insumo)
                    .cantidadRequerida(cantidadRequerida)
                    .cantidadUsada(suficiente ? cantidadRequerida : BigDecimal.ZERO)
                    .precioUnidadBaseSnapshot(precioSnapshot)
                    .costoLinea(costoLinea)
                    .suficiente(suficiente)
                    .build());
        }
        return detalles;
    }

    /**
     * Aplica los efectos de una produccion completada: descuenta el stock de
     * los insumos, aumenta el stock del producto (solo en produccion manual) y
     * genera el gasto automatico de materia prima.
     */
    private void aplicarProduccion(Produccion produccion) {
        for (DetalleProduccion detalle : produccion.getDetalles()) {
            Insumo insumo = inventarioService.bloquearInsumo(detalle.getInsumo().getId());
            inventarioService.descontarInsumo(insumo, detalle.getCantidadRequerida());
        }

        // Las producciones automaticas de ventas no tocan el stock del producto:
        // el inventario real de un producto bajo pedido son sus insumos.
        if (!produccion.esAutomatica()) {
            Producto producto = inventarioService.bloquearProducto(produccion.getProducto().getId());
            producto.setStockActual(producto.getStockActual() + produccion.getCantidad());
            productoRepository.save(producto);
        }

        BigDecimal costoTotal = produccion.getCostoTotal();
        if (costoTotal != null && costoTotal.compareTo(BigDecimal.ZERO) > 0) {
            Producto producto = produccion.getProducto();
            Gasto gasto = Gasto.builder()
                    .descripcion("Materia prima - " + producto.getNombre() + " x" + produccion.getCantidad()
                            + " (produccion #" + produccion.getId() + ")")
                    .monto(costoTotal.setScale(2, RoundingMode.HALF_UP))
                    .categoria(CategoriaGasto.MATERIA_PRIMA)
                    .fecha(produccion.getFecha())
                    .produccion(produccion)
                    .build();
            gastoRepository.save(gasto);
        }
    }

    @Transactional
    public void eliminar(Long id) {
        Produccion produccion = produccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Produccion", id));

        if (produccion.getEstado() == EstadoProduccion.COMPLETADA) {
            revertirCompletada(produccion, true);
        }

        produccionRepository.deleteById(id);
    }

    /**
     * Revierte los efectos de una produccion completada. Devuelve exactamente
     * la cantidad que se descontó (cantidadUsada) y elimina el gasto asociado.
     */
    private void revertirCompletada(Produccion produccion, boolean devolverProducto) {
        for (DetalleProduccion detalle : produccion.getDetalles()) {
            Insumo insumo = inventarioService.bloquearInsumo(detalle.getInsumo().getId());
            inventarioService.reponerInsumo(insumo, detalle.getCantidadUsada());
        }

        if (devolverProducto) {
            Producto producto = inventarioService.bloquearProducto(produccion.getProducto().getId());
            producto.setStockActual(producto.getStockActual() - produccion.getCantidad());
            productoRepository.save(producto);
        }

        gastoRepository.deleteByProduccionId(produccion.getId());
    }
}
