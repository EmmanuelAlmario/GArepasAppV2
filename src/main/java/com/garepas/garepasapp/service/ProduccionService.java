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

        Produccion produccion = Produccion.builder()
                .producto(producto)
                .cantidad(request.cantidad())
                .fecha(LocalDateTime.now())
                .costoTotal(BigDecimal.ZERO)
                .estado(EstadoProduccion.PENDIENTE)
                .build();

        // Calcular detalles, suficiencia y costo (sin descontar stock aún)
        List<DetalleProduccion> detalles = construirDetalles(produccion, producto, request.cantidad());
        BigDecimal costoTotal = detalles.stream()
                .map(DetalleProduccion::getCostoLinea)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        produccion.setDetalles(detalles);
        produccion.setCostoTotal(costoTotal);
        produccion.setEstado(EstadoProduccion.PENDIENTE);

        // Se guarda primero para que la produccion tenga id: el gasto de materia
        // prima generado la referencia (produccionId) y la menciona en su descripcion.
        Produccion guardada = produccionRepository.save(produccion);

        boolean todosSuficientes = detalles.stream().allMatch(DetalleProduccion::getSuficiente);
        if (todosSuficientes) {
            aplicarProduccion(guardada);
            guardada.setEstado(EstadoProduccion.COMPLETADA);
            guardada = produccionRepository.save(guardada);
        }
        return ProduccionResponse.desde(guardada);
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

        producto.setStockActual(producto.getStockActual() + produccion.getCantidad());
        productoRepository.save(producto);

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
            for (DetalleProduccion detalle : produccion.getDetalles()) {
                Insumo insumo = insumoRepository.findByIdConLock(detalle.getInsumo().getId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Insumo", detalle.getInsumo().getId()));
                insumo.setStockActual(insumo.getStockActual().add(detalle.getCantidadUsada()));
                insumoRepository.save(insumo);
            }

            Producto producto = productoRepository.findByIdConLock(produccion.getProducto().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto", produccion.getProducto().getId()));
            producto.setStockActual(producto.getStockActual() - produccion.getCantidad());
            productoRepository.save(producto);

            gastoRepository.deleteByProduccionId(id);
        }

        produccionRepository.deleteById(id);
    }
}
