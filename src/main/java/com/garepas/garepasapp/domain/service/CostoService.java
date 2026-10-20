package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.model.DetalleReceta;
import com.garepas.garepasapp.domain.model.Producto;
import com.garepas.garepasapp.domain.model.Receta;
import com.garepas.garepasapp.domain.repository.ProductoRepository;
import com.garepas.garepasapp.domain.repository.RecetaRepository;
import com.garepas.garepasapp.web.view.CostoProductoResponse;
import com.garepas.garepasapp.web.view.CostoRecetaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

  












@Service
@RequiredArgsConstructor
public class CostoService {

    private static final int ESCALA_COSTO = 6;
    private static final int ESCALA_PRECIO = 2;

    private final RecetaRepository recetaRepository;
    private final ProductoRepository productoRepository;

      


    public static BigDecimal costoDeReceta(Receta receta) {
        if (receta == null || receta.getDetalles() == null) {
            return BigDecimal.ZERO;
        }
        return receta.getDetalles().stream()
                .map(CostoService::subtotalDetalle)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

      
    public static BigDecimal subtotalDetalle(DetalleReceta detalle) {
        if (detalle.getInsumo() == null || detalle.getInsumo().getPrecioPorUnidadBase() == null) {
            return BigDecimal.ZERO;
        }
        return detalle.getCantidad().multiply(detalle.getInsumo().getPrecioPorUnidadBase());
    }

      



    public static BigDecimal rendimientoDe(Receta receta) {
        if (receta == null || receta.getRendimiento() == null
                || receta.getRendimiento().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return receta.getRendimiento();
    }

      



    public static BigDecimal costoUnitarioProducto(Producto producto) {
        if (producto == null || producto.getReceta() == null) {
            return BigDecimal.ZERO;
        }
        return costoDeReceta(producto.getReceta())
                .divide(rendimientoDe(producto.getReceta()), ESCALA_COSTO, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public CostoRecetaResponse calcularCostoReceta(Long recetaId) {
        Receta receta = recetaRepository.findById(recetaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta", recetaId));
        List<com.garepas.garepasapp.web.view.DetalleRecetaResponse> desglose = receta.getDetalles().stream()
                .map(com.garepas.garepasapp.web.view.DetalleRecetaResponse::desde)
                .toList();
        BigDecimal costoTotal = costoDeReceta(receta);
        BigDecimal costoUnitario = costoTotal.divide(rendimientoDe(receta), ESCALA_COSTO, RoundingMode.HALF_UP);
        return new CostoRecetaResponse(receta.getId(), receta.getNombre(), costoTotal, costoUnitario, desglose);
    }

    @Transactional(readOnly = true)
    public CostoProductoResponse calcularCostoProducto(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", productoId));
        BigDecimal costoUnitario = costoUnitarioProducto(producto);
        BigDecimal precioVenta = producto.getPrecioVenta() == null ? BigDecimal.ZERO : producto.getPrecioVenta();
        BigDecimal margen = precioVenta.subtract(costoUnitario);
        BigDecimal margenPct = margen.multiply(BigDecimal.valueOf(100))
                .divide(precioVenta, 2, RoundingMode.HALF_UP);
        return new CostoProductoResponse(producto.getId(), producto.getNombre(),
                producto.getReceta() == null ? null : producto.getReceta().getId(),
                costoUnitario, precioVenta, margen, margenPct);
    }

      




    @Transactional(readOnly = true)
    public BigDecimal sugerirPrecioVenta(Long productoId, BigDecimal margenPorcentaje) {
        if (margenPorcentaje == null || margenPorcentaje.compareTo(BigDecimal.ZERO) < 0) {
            throw new OperacionInvalidaException("El margen debe ser un porcentaje mayor o igual a cero");
        }
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", productoId));
        BigDecimal costo = costoUnitarioProducto(producto);
        BigDecimal factor = BigDecimal.ONE.add(margenPorcentaje.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        return costo.multiply(factor).setScale(ESCALA_PRECIO, RoundingMode.HALF_UP);
    }
}
