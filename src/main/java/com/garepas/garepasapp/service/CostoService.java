package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.response.*;
import com.garepas.garepasapp.entity.DetalleReceta;
import com.garepas.garepasapp.entity.Producto;
import com.garepas.garepasapp.entity.Receta;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.repository.ProductoRepository;
import com.garepas.garepasapp.repository.RecetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Servicio dedicado al cálculo de costos de producción a partir de recetas.
 *
 * Fórmula: costoReceta = Σ (detalle.cantidad × insumo.precioPorGramo).
 * Todas las cantidades se persisten en unidad base (gramo / mililitro / unidad),
 * por lo que la multiplicación es directa sin conversiones.
 */
@Service
@RequiredArgsConstructor
public class CostoService {

    private final RecetaRepository recetaRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public CostoRecetaResponse calcularCostoReceta(Long recetaId) {
        Receta receta = recetaRepository.findById(recetaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta", recetaId));
        return construir(receta);
    }

    @Transactional(readOnly = true)
    public CostoProductoResponse calcularCostoProducto(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", productoId));

        BigDecimal costo = costoDeReceta(producto.getReceta());
        BigDecimal precioVenta = producto.getPrecioVenta() != null ? producto.getPrecioVenta() : BigDecimal.ZERO;
        BigDecimal margen = precioVenta.subtract(costo);
        BigDecimal margenPct = precioVenta.compareTo(BigDecimal.ZERO) > 0
                ? margen.multiply(BigDecimal.valueOf(100)).divide(precioVenta, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new CostoProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getReceta() != null ? producto.getReceta().getId() : null,
                costo,
                precioVenta,
                margen,
                margenPct
        );
    }

    @Transactional(readOnly = true)
    public SugerenciaPrecioResponse sugerirPrecioVenta(Long productoId, BigDecimal margenObjetivo) {
        if (margenObjetivo == null || margenObjetivo.compareTo(BigDecimal.ZERO) < 0) {
            throw new OperacionInvalidaException("El margen debe ser mayor o igual a cero");
        }
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", productoId));

        BigDecimal costo = costoDeReceta(producto.getReceta());
        if (costo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OperacionInvalidaException(
                    "No se puede sugerir precio: el producto '" + producto.getNombre() +
                    "' no tiene receta o su costo es cero.");
        }
        BigDecimal precioSugerido = costo.multiply(BigDecimal.ONE.add(margenObjetivo))
                .setScale(2, RoundingMode.HALF_UP);
        return new SugerenciaPrecioResponse(producto.getId(), costo, margenObjetivo, precioSugerido);
    }

    // ------------- helpers -------------

    public static BigDecimal costoDeReceta(Receta receta) {
        if (receta == null || receta.getDetalles() == null || receta.getDetalles().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return receta.getDetalles().stream()
                .map(CostoService::subtotalDetalle)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal subtotalDetalle(DetalleReceta detalle) {
        BigDecimal precio = detalle.getInsumo().getPrecioPorGramo();
        return detalle.getCantidad().multiply(precio);
    }

    private CostoRecetaResponse construir(Receta receta) {
        var desglose = receta.getDetalles().stream()
                .map(DetalleRecetaResponse::desde)
                .toList();
        BigDecimal total = desglose.stream()
                .map(DetalleRecetaResponse::subtotalCosto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CostoRecetaResponse(receta.getId(), receta.getNombre(), total, desglose);
    }
}
