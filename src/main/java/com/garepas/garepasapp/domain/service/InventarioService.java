package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.model.AjusteInventario;
import com.garepas.garepasapp.domain.model.Insumo;
import com.garepas.garepasapp.domain.model.Producto;
import com.garepas.garepasapp.domain.repository.AjusteInventarioRepository;
import com.garepas.garepasapp.domain.repository.InsumoRepository;
import com.garepas.garepasapp.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

  




















@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InsumoRepository insumoRepository;
    private final ProductoRepository productoRepository;
    private final AjusteInventarioRepository ajusteInventarioRepository;

      




    @Transactional
    public Map<Long, Insumo> bloquearInsumos(Collection<Long> ids) {
        List<Long> ordenados = ids.stream().filter(Objects::nonNull).distinct().sorted().toList();
        if (ordenados.isEmpty()) {
            return Map.of();
        }
        Map<Long, Insumo> resultado = new LinkedHashMap<>();
        for (Insumo insumo : insumoRepository.findAllByIdConLock(ordenados)) {
            resultado.put(insumo.getId(), insumo);
        }
        for (Long id : ordenados) {
            if (!resultado.containsKey(id)) {
                throw new RecursoNoEncontradoException("Insumo", id);
            }
        }
        return resultado;
    }

      
    @Transactional
    public Insumo bloquearInsumo(Long id) {
        return insumoRepository.findByIdConLock(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo", id));
    }

      
    @Transactional
    public Map<Long, Producto> bloquearProductos(Collection<Long> ids) {
        List<Long> ordenados = ids.stream().filter(Objects::nonNull).distinct().sorted().toList();
        if (ordenados.isEmpty()) {
            return Map.of();
        }
        Map<Long, Producto> resultado = new LinkedHashMap<>();
        for (Producto producto : productoRepository.findAllByIdConLock(ordenados)) {
            resultado.put(producto.getId(), producto);
        }
        for (Long id : ordenados) {
            if (!resultado.containsKey(id)) {
                throw new RecursoNoEncontradoException("Producto", id);
            }
        }
        return resultado;
    }

      
    @Transactional
    public Producto bloquearProducto(Long id) {
        return productoRepository.findByIdConLock(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
    }

      



    @Transactional
    public void descontarInsumo(Insumo insumo, BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal nuevo = insumo.getStockActual().subtract(cantidad);
        if (nuevo.compareTo(BigDecimal.ZERO) < 0) {
            throw new OperacionInvalidaException(
                    "Stock insuficiente de '" + insumo.getNombre() + "'. Disponible: "
                            + insumo.stockEnPresentacion() + ", requerido: " + cantidad);
        }
        insumo.setStockActual(nuevo);
        insumoRepository.save(insumo);
    }

      
    @Transactional
    public void reponerInsumo(Insumo insumo, BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        insumo.setStockActual(insumo.getStockActual().add(cantidad));
        insumoRepository.save(insumo);
    }

      
    @Transactional
    public void descontarProducto(Producto producto, Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            return;
        }
        int nuevo = producto.getStockActual() - cantidad;
        if (nuevo < 0) {
            throw new OperacionInvalidaException(
                    "Stock insuficiente de '" + producto.getNombre() + "'. Disponible: "
                            + producto.getStockActual() + ", solicitado: " + cantidad);
        }
        producto.setStockActual(nuevo);
        productoRepository.save(producto);
    }

      
    @Transactional
    public void reponerProducto(Producto producto, Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            return;
        }
        producto.setStockActual(producto.getStockActual() + cantidad);
        productoRepository.save(producto);
    }

      







    @Transactional
    public void ajustarInsumo(Insumo insumo, BigDecimal delta, String motivo, String usuario) {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new OperacionInvalidaException("La cantidad de ajuste no puede ser cero.");
        }
        BigDecimal nuevo = insumo.getStockActual().add(delta);
        if (nuevo.compareTo(BigDecimal.ZERO) < 0) {
            throw new OperacionInvalidaException(
                    "El ajuste dejaria el stock de '" + insumo.getNombre() + "' en negativo. Stock actual: "
                            + insumo.stockEnPresentacion() + ", ajuste: " + delta);
        }
        insumo.setStockActual(nuevo);
        insumoRepository.save(insumo);

        BigDecimal valor = delta.abs().multiply(insumo.getPrecioPorUnidadBase()).setScale(2, RoundingMode.HALF_UP);
        ajusteInventarioRepository.save(AjusteInventario.builder()
                .insumo(insumo)
                .delta(delta)
                .stockResultante(nuevo)
                .valor(valor)
                .motivo(motivo == null || motivo.isBlank() ? "Ajuste manual" : motivo)
                .usuario(usuario == null || usuario.isBlank() ? "sistema" : usuario)
                .fecha(LocalDateTime.now())
                .build());
    }
}
