package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.request.ProductoRequest;
import com.garepas.garepasapp.dto.response.ProductoResponse;
import com.garepas.garepasapp.entity.Producto;
import com.garepas.garepasapp.entity.Receta;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.repository.ProductoRepository;
import com.garepas.garepasapp.repository.RecetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final RecetaRepository recetaRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAll()
                .stream()
                .map(ProductoResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarActivos() {
        return productoRepository.findByActivoTrue()
                .stream()
                .map(ProductoResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse buscarPorId(Long id) {
        return productoRepository.findById(id)
                .map(ProductoResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        if (productoRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new RecursoDuplicadoException("Producto", "nombre", request.nombre());
        }
        Receta receta = resolverReceta(request.recetaId());
        Producto producto = Producto.builder()
                .nombre(request.nombre())
                .stockActual(request.stockActual() != null ? request.stockActual() : 0)
                .stockMinimo(request.stockMinimo())
                .precioVenta(request.precioVenta())
                .receta(receta)
                .activo(request.activo())
                .build();
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    /**
     * Actualiza los campos permitidos del producto, incluyendo un ajuste manual de
     * stockActual (el formulario lo envía siempre con el valor actual del producto).
     */
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));

        if (!producto.getNombre().equalsIgnoreCase(request.nombre()) &&
                productoRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new RecursoDuplicadoException("Producto", "nombre", request.nombre());
        }

        Receta receta = resolverReceta(request.recetaId());

        producto.setNombre(request.nombre());
        producto.setPrecioVenta(request.precioVenta());
        producto.setReceta(receta);
        producto.setStockActual(request.stockActual());
        producto.setStockMinimo(request.stockMinimo());
        producto.setActivo(request.activo());
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    /**
     * Ajuste manual de stock (auditable). Usar sólo para conciliaciones
     * físicas / mermas — el flujo normal es Venta / Produccion.
     */
    @Transactional
    public ProductoResponse ajustarStock(Long id, Integer delta, String motivo) {
        if (delta == null || delta == 0) {
            throw new OperacionInvalidaException("El delta de ajuste no puede ser cero");
        }
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
        int nuevo = producto.getStockActual() + delta;
        if (nuevo < 0) {
            throw new OperacionInvalidaException(
                    "El ajuste dejaría el stock en negativo. Actual: " + producto.getStockActual() + ", delta: " + delta);
        }
        producto.setStockActual(nuevo);
        // motivo se recibe para trazabilidad futura (auditoría), aún no persistido.
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Producto", id);
        }
        productoRepository.deleteById(id);
    }

    private Receta resolverReceta(Long recetaId) {
        if (recetaId == null) return null;
        return recetaRepository.findById(recetaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta", recetaId));
    }
}
