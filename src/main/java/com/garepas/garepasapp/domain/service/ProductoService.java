package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.ProductoCommand;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.model.Producto;
import com.garepas.garepasapp.domain.model.Receta;
import com.garepas.garepasapp.domain.repository.ProductoRepository;
import com.garepas.garepasapp.domain.repository.RecetaRepository;
import com.garepas.garepasapp.web.view.MenuPublicoItemResponse;
import com.garepas.garepasapp.web.view.ProductoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final RecetaRepository recetaRepository;
    private final InventarioService inventarioService;

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAll().stream().map(ProductoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarActivos() {
        return productoRepository.findByActivoTrue().stream().map(ProductoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<MenuPublicoItemResponse> listarMenuPublico() {
        return productoRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(MenuPublicoItemResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse buscarPorId(Long id) {
        return productoRepository.findById(id)
                .map(ProductoResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
    }

    @Transactional
    public ProductoResponse crear(ProductoCommand command) {
        if (productoRepository.existsByNombreIgnoreCase(command.nombre())) {
            throw new RecursoDuplicadoException("Producto", "nombre", command.nombre());
        }
        Receta receta = resolverReceta(command.recetaId());
        Producto producto = Producto.builder()
                .nombre(command.nombre())
                .stockActual(command.stockActual() != null ? command.stockActual() : 0)
                .stockMinimo(command.stockMinimo())
                .precioVenta(command.precioVenta())
                .receta(receta)
                .bajoPedido(validarBajoPedido(Boolean.TRUE.equals(command.bajoPedido()), receta, command.nombre()))
                .activo(command.activo() == null || command.activo())
                .build();
        return ProductoResponse.desde(productoRepository.save(producto));
    }

      




    @Transactional
    public ProductoResponse actualizar(Long id, ProductoCommand command) {
        Producto producto = inventarioService.bloquearProducto(id);

        if (!producto.getNombre().equalsIgnoreCase(command.nombre())
                && productoRepository.existsByNombreIgnoreCase(command.nombre())) {
            throw new RecursoDuplicadoException("Producto", "nombre", command.nombre());
        }

        Receta receta = resolverReceta(command.recetaId());

        producto.setNombre(command.nombre());
        producto.setPrecioVenta(command.precioVenta());
        producto.setReceta(receta);
        producto.setStockMinimo(command.stockMinimo());
        producto.setBajoPedido(validarBajoPedido(Boolean.TRUE.equals(command.bajoPedido()), receta, command.nombre()));
        producto.setActivo(command.activo() == null || command.activo());

        if (command.stockActual() != null && command.stockActual() != producto.getStockActual()) {
            int delta = command.stockActual() - producto.getStockActual();
            if (producto.getStockActual() + delta < 0) {
                throw new OperacionInvalidaException(
                        "El stock del producto '" + producto.getNombre() + "' no puede quedar en negativo");
            }
            producto.setStockActual(producto.getStockActual() + delta);
        }

        return ProductoResponse.desde(productoRepository.save(producto));
    }

      




    private Boolean validarBajoPedido(boolean bajoPedido, Receta receta, String nombre) {
        if (bajoPedido && receta == null) {
            throw new OperacionInvalidaException(
                    "El producto bajo pedido '" + nombre + "' requiere una receta asignada");
        }
        return bajoPedido;
    }

      



    @Transactional
    public ProductoResponse ajustarStock(Long id, Integer delta, String motivo) {
        if (delta == null || delta == 0) {
            throw new OperacionInvalidaException("El delta de ajuste no puede ser cero");
        }
        Producto producto = inventarioService.bloquearProducto(id);
        int nuevo = producto.getStockActual() + delta;
        if (nuevo < 0) {
            throw new OperacionInvalidaException(
                    "El ajuste dejaria el stock en negativo. Actual: " + producto.getStockActual() + ", delta: " + delta);
        }
        producto.setStockActual(nuevo);
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));

        if (productoRepository.countVentasQueUsan(id) > 0) {
            throw new OperacionInvalidaException(
                    "El producto '" + producto.getNombre() + "' tiene ventas registradas. Desactivalo en lugar de eliminarlo.");
        }
        if (productoRepository.countProduccionesQueUsan(id) > 0) {
            throw new OperacionInvalidaException(
                    "El producto '" + producto.getNombre() + "' tiene producciones registradas. Desactivalo en lugar de eliminarlo.");
        }
        productoRepository.deleteById(id);
    }

    private Receta resolverReceta(Long recetaId) {
        if (recetaId == null) {
            return null;
        }
        return recetaRepository.findById(recetaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta", recetaId));
    }
}
