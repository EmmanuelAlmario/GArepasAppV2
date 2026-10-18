package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.request.ProduccionRequest;
import com.garepas.garepasapp.dto.response.ProduccionResponse;
import com.garepas.garepasapp.entity.*;
import com.garepas.garepasapp.enums.CategoriaGasto;
import com.garepas.garepasapp.enums.EstadoProduccion;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.repository.GastoRepository;
import com.garepas.garepasapp.repository.InsumoRepository;
import com.garepas.garepasapp.repository.ProduccionRepository;
import com.garepas.garepasapp.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProduccionService {

    private final ProduccionRepository produccionRepository;
    private final ProductoRepository productoRepository;
    private final InsumoRepository insumoRepository;
    private final GastoRepository gastoRepository;

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

    @Transactional
    public ProduccionResponse registrar(ProduccionRequest request) {
        Producto producto = productoRepository.findById(request.productoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", request.productoId()));

        if (producto.getReceta() == null) {
            throw new OperacionInvalidaException(
                    "El producto '" + producto.getNombre() + "' no tiene una receta asignada");
        }

        // Sin ventaId (produccion manual). Si faltan insumos queda PENDIENTE,
        // para completarla luego con verificarYCompletar cuando haya stock.
        return ProduccionResponse.desde(
                crearYAplicar(producto, request.cantidad(), null, false));
    }

    /**
     * Produccion automatica generada al vender un producto bajo pedido.
     * Pasa por el mismo pipeline de costo/descuento que una produccion manual
     * (una sola fuente de verdad para el consumo de insumos), pero exige
     * insumos suficientes: una venta no puede quedar PENDIENTE. Lanza
     * OperacionInvalidaException con los faltantes si no alcanza el stock.
     */
    @Transactional
    public Produccion registrarParaVenta(Long productoId, Integer cantidad, Long ventaId) {
        Producto producto = productoRepository.findByIdConLock(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", productoId));

        if (!Boolean.TRUE.equals(producto.getBajoPedido()) || producto.getReceta() == null) {
            throw new OperacionInvalidaException(
                    "El producto '" + producto.getNombre() + "' no es un producto bajo pedido con receta");
        }
        return crearYAplicar(producto, cantidad, ventaId, true);
    }

    /**
     * Revierte las producciones automaticas asociadas a una venta anulada:
     * devuelve los insumos y elimina el gasto de materia prima. No devuelve
     * stock de producto porque la produccion automatica no lo afecta (la
     * venta lo descuenta y el stock bruto queda igual que antes).
     */
    @Transactional
    public void revertirDeVenta(Long ventaId) {
        for (Produccion produccion : produccionRepository.findByVentaId(ventaId)) {
            if (produccion.getEstado() == EstadoProduccion.COMPLETADA) {
                revertirCompletada(produccion, false);
            }
            produccionRepository.deleteById(produccion.getId());
        }
    }

    private Produccion crearYAplicar(Producto producto, Integer cantidad, Long ventaId,
                                     boolean exigirSuficiencia) {
        Produccion produccion = Produccion.builder()
                .producto(producto)
                .cantidad(cantidad)
                .fecha(LocalDateTime.now())
                .costoTotal(BigDecimal.ZERO)
                .estado(EstadoProduccion.PENDIENTE)
                .ventaId(ventaId)
                .build();

        // Calcular detalles, suficiencia y costo (sin descontar stock aún)
        List<DetalleProduccion> detalles = construirDetalles(produccion, producto, cantidad);
        BigDecimal costoTotal = detalles.stream()
                .map(DetalleProduccion::getCostoLinea)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        produccion.setDetalles(detalles);
        produccion.setCostoTotal(costoTotal);

        boolean todosSuficientes = detalles.stream().allMatch(DetalleProduccion::getSuficiente);
        if (!todosSuficientes && exigirSuficiencia) {
            throw new OperacionInvalidaException(mensajeInsumosInsuficientes(producto, detalles));
        }

        // Se guarda primero para que la produccion tenga id: el gasto de materia
        // prima generado la referencia (produccionId) y la menciona en su descripcion.
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
                .collect(java.util.stream.Collectors.joining(", "));
        return "Insumos insuficientes para '" + producto.getNombre()
                + "'. Faltan: " + faltantes + ". Reabastece el inventario antes de vender.";
    }

    /**
     * Re-verifica el stock de insumos de una producción PENDIENTE.
     * Si todos los insumos ya son suficientes, descuenta stock, crea gasto
     * y marca la producción como COMPLETADA. Devuelve la producción actualizada.
     */
    @Transactional
    public ProduccionResponse verificarYCompletar(Long id) {
        Produccion produccion = produccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Produccion", id));

        if (produccion.getEstado() == EstadoProduccion.COMPLETADA) {
            return ProduccionResponse.desde(produccion);
        }

        // Recalcular suficiencia con el stock actual de los insumos
        List<DetalleProduccion> detalles = construirDetalles(
                produccion, produccion.getProducto(), produccion.getCantidad());
        BigDecimal costoTotal = detalles.stream()
                .map(DetalleProduccion::getCostoLinea)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Reemplazar detalles viejos por los recalculados
        produccion.getDetalles().clear();
        produccion.getDetalles().addAll(detalles);
        produccion.setCostoTotal(costoTotal);

        boolean todosSuficientes = detalles.stream().allMatch(DetalleProduccion::getSuficiente);
        if (todosSuficientes) {
            aplicarProduccion(produccion);
            produccion.setEstado(EstadoProduccion.COMPLETADA);
        } else {
            produccion.setEstado(EstadoProduccion.PENDIENTE);
        }

        Produccion guardada = produccionRepository.save(produccion);
        return ProduccionResponse.desde(guardada);
    }

    /**
     * Construye los detalles de producción recalculando suficiencia y costo
     * con el stock actual del insumo. No muta el stock de los insumos.
     */
    private List<DetalleProduccion> construirDetalles(Produccion produccion, Producto producto, Integer cantidad) {
        List<DetalleProduccion> detalles = new ArrayList<>();
        for (DetalleReceta detalleReceta : producto.getReceta().getDetalles()) {
            Insumo insumo = insumoRepository.findByIdConLock(detalleReceta.getInsumo().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Insumo", detalleReceta.getInsumo().getId()));
            BigDecimal cantidadRequerida = detalleReceta.getCantidad()
                    .multiply(BigDecimal.valueOf(cantidad));
            boolean suficiente = insumo.getStockActual().compareTo(cantidadRequerida) >= 0;

            BigDecimal precioSnap = insumo.getPrecioPorGramo();
            BigDecimal costoLinea = cantidadRequerida.multiply(precioSnap);

            detalles.add(DetalleProduccion.builder()
                    .produccion(produccion)
                    .insumo(insumo)
                    .cantidadRequerida(cantidadRequerida)
                    .cantidadUsada(suficiente ? cantidadRequerida : BigDecimal.ZERO)
                    .precioPorGramoSnapshot(precioSnap)
                    .costoLinea(costoLinea)
                    .suficiente(suficiente)
                    .build());
        }
        return detalles;
    }

    /**
     * Aplica los efectos de una producción COMPLETADA:
     * - Descuenta el stock de cada insumo usado.
     * - Aumenta el stock del producto.
     * - Crea el gasto automático de materia prima asociado.
     */
    private void aplicarProduccion(Produccion produccion) {
        Producto producto = productoRepository.findByIdConLock(produccion.getProducto().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", produccion.getProducto().getId()));

        for (DetalleProduccion detalle : produccion.getDetalles()) {
            Insumo insumo = insumoRepository.findByIdConLock(detalle.getInsumo().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Insumo", detalle.getInsumo().getId()));
            insumo.setStockActual(insumo.getStockActual().subtract(detalle.getCantidadRequerida()));
            insumoRepository.save(insumo);
        }

        // Las producciones automaticas de ventas no tocan el stock del
        // producto (el inventario real de un producto bajo pedido son sus
        // insumos); solo las producciones manuales lo aumentan.
        if (produccion.getVentaId() == null) {
            producto.setStockActual(producto.getStockActual() + produccion.getCantidad());
            productoRepository.save(producto);
        }

        BigDecimal costoTotal = produccion.getCostoTotal();
        if (costoTotal != null && costoTotal.compareTo(BigDecimal.ZERO) > 0) {
            Gasto gasto = Gasto.builder()
                    .descripcion("Materia prima — " + producto.getNombre() + " x" + produccion.getCantidad()
                            + " (producción #" + produccion.getId() + ")")
                    .monto(costoTotal.setScale(2, java.math.RoundingMode.HALF_UP))
                    .categoria(CategoriaGasto.MATERIA_PRIMA)
                    .fecha(produccion.getFecha())
                    .produccionId(produccion.getId())
                    .build();
            gastoRepository.save(gasto);
        }
    }

    @Transactional
    public void eliminar(Long id) {
        Produccion produccion = produccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Produccion", id));

        // Solo se revierte stock si la producción estaba COMPLETADA
        if (produccion.getEstado() == EstadoProduccion.COMPLETADA) {
            revertirCompletada(produccion, true);
        }

        produccionRepository.deleteById(id);
    }

    /**
     * Revierte los efectos de una producción COMPLETADA: devuelve el stock de
     * los insumos usados, (opcionalmente) el stock del producto y elimina el
     * gasto de materia prima asociado. Para producciones automáticas de ventas
     * el stock del producto no se devuelve (la venta lo reintegra).
     */
    private void revertirCompletada(Produccion produccion, boolean devolverProducto) {
        for (DetalleProduccion detalle : produccion.getDetalles()) {
            Insumo insumo = insumoRepository.findByIdConLock(detalle.getInsumo().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Insumo", detalle.getInsumo().getId()));
            insumo.setStockActual(insumo.getStockActual().add(detalle.getCantidadUsada()));
            insumoRepository.save(insumo);
        }

        if (devolverProducto) {
            Producto producto = productoRepository.findByIdConLock(produccion.getProducto().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto", produccion.getProducto().getId()));
            producto.setStockActual(producto.getStockActual() - produccion.getCantidad());
            productoRepository.save(producto);
        }

        gastoRepository.deleteByProduccionId(produccion.getId());
    }
}
