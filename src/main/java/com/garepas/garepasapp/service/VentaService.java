package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.request.VentaRequest;
import com.garepas.garepasapp.dto.response.VentaResponse;
import com.garepas.garepasapp.entity.DetalleVenta;
import com.garepas.garepasapp.entity.Producto;
import com.garepas.garepasapp.entity.Venta;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.repository.JornadaRepository;
import com.garepas.garepasapp.repository.ProductoRepository;
import com.garepas.garepasapp.repository.VentaRepository;
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
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final JornadaRepository jornadaRepository;

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
        java.util.List<Long> ids = page.getContent().stream().map(Venta::getId).toList();
        if (!ids.isEmpty()) {
            java.util.Map<Long, List<DetalleVenta>> detallesPorVenta =
                    ventaRepository.findDetallesPorVentaIds(ids).stream()
                            .collect(java.util.stream.Collectors.groupingBy(d -> d.getVenta().getId()));
            page.getContent().forEach(v -> v.setDetalles(
                    detallesPorVenta.getOrDefault(v.getId(), java.util.List.of())));
        }
        return page.map(VentaResponse::desde);
    }

    @Transactional(readOnly = true)
    public VentaResponse buscarPorId(Long id) {
        return ventaRepository.findById(id)
                .map(VentaResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", id));
    }

    @Transactional
    public VentaResponse registrar(VentaRequest request) {
        if (request.detalles() == null || request.detalles().isEmpty()) {
            throw new OperacionInvalidaException("La venta debe incluir al menos un detalle");
        }

        Venta venta = Venta.builder()
                .fecha(LocalDateTime.now())
                .total(BigDecimal.ZERO)
                .nombreCliente(request.nombreCliente())
                .jornadaId(obtenerJornadaActivaId())
                .build();

        List<DetalleVenta> detalles = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (var detalleRequest : request.detalles()) {
            Producto producto = productoRepository.findByIdConLock(detalleRequest.productoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto", detalleRequest.productoId()));

            if (!producto.getActivo()) {
                throw new OperacionInvalidaException(
                        "El producto '" + producto.getNombre() + "' no está activo");
            }

            if (producto.getStockActual() < detalleRequest.cantidad()) {
                throw new OperacionInvalidaException(
                        "Stock insuficiente para el producto '" + producto.getNombre() +
                                "'. Disponible: " + producto.getStockActual() +
                                ", Solicitado: " + detalleRequest.cantidad());
            }

            // Precio del servidor (snapshot); no confiar en el cliente.
            BigDecimal precioUnitario = producto.getPrecioVenta();
            BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(detalleRequest.cantidad()));

            producto.setStockActual(producto.getStockActual() - detalleRequest.cantidad());
            productoRepository.save(producto);

            detalles.add(DetalleVenta.builder()
                    .venta(venta)
                    .producto(producto)
                    .cantidad(detalleRequest.cantidad())
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotal)
                    .build());
            total = total.add(subtotal);
        }

        venta.setTotal(total);
        venta.setDetalles(detalles);
        return VentaResponse.desde(ventaRepository.save(venta));
    }

    @Transactional
    public void eliminar(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", id));

        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = productoRepository.findByIdConLock(detalle.getProducto().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto", detalle.getProducto().getId()));
            producto.setStockActual(producto.getStockActual() + detalle.getCantidad());
            productoRepository.save(producto);
        }

        ventaRepository.deleteById(id);
    }

    private Long obtenerJornadaActivaId() {
        return jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc()
                .map(com.garepas.garepasapp.entity.Jornada::getId)
                .orElse(null);
    }
}
