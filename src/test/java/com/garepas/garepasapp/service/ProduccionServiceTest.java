package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.request.ProduccionRequest;
import com.garepas.garepasapp.dto.response.ProduccionResponse;
import com.garepas.garepasapp.entity.DetalleReceta;
import com.garepas.garepasapp.entity.Gasto;
import com.garepas.garepasapp.entity.Insumo;
import com.garepas.garepasapp.entity.Producto;
import com.garepas.garepasapp.entity.Produccion;
import com.garepas.garepasapp.entity.Receta;
import com.garepas.garepasapp.enums.CategoriaGasto;
import com.garepas.garepasapp.enums.EstadoProduccion;
import com.garepas.garepasapp.enums.UnidadMedida;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.repository.GastoRepository;
import com.garepas.garepasapp.repository.InsumoRepository;
import com.garepas.garepasapp.repository.ProduccionRepository;
import com.garepas.garepasapp.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Reglas de producción: sin receta no se puede registrar; con insumos
 * suficientes descuenta stock, suma producto y crea el gasto de materia
 * prima; con insumos insuficientes queda PENDIENTE y no toca nada.
 */
class ProduccionServiceTest {

    private ProduccionRepository produccionRepository;
    private ProductoRepository productoRepository;
    private InsumoRepository insumoRepository;
    private GastoRepository gastoRepository;
    private ProduccionService produccionService;

    @BeforeEach
    void setUp() {
        produccionRepository = mock(ProduccionRepository.class);
        productoRepository = mock(ProductoRepository.class);
        insumoRepository = mock(InsumoRepository.class);
        gastoRepository = mock(GastoRepository.class);
        produccionService = new ProduccionService(
                produccionRepository, productoRepository, insumoRepository, gastoRepository);
        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> {
            Produccion produccion = inv.getArgument(0);
            if (produccion.getId() == null) {
                produccion.setId(1L);
            }
            return produccion;
        });
    }

    private Insumo insumo(Long id, String nombre, String stock, String precio) {
        return Insumo.builder()
                .id(id)
                .nombre(nombre)
                .categoria("Granos")
                .marca("Genérica")
                .stockActual(new BigDecimal(stock))
                .precioPorGramo(new BigDecimal(precio))
                .unidadMedida(UnidadMedida.GRAMO)
                .activo(true)
                .build();
    }

    private Producto productoConReceta(Insumo insumo, String cantidadPorUnidad) {
        DetalleReceta detalle = DetalleReceta.builder()
                .insumo(insumo)
                .cantidad(new BigDecimal(cantidadPorUnidad))
                .unidadMedida(UnidadMedida.GRAMO)
                .build();
        Receta receta = Receta.builder().nombre("Receta de prueba").build();
        detalle.setReceta(receta);
        receta.getDetalles().add(detalle);
        return Producto.builder()
                .id(5L)
                .nombre("Arepa de prueba")
                .stockActual(0)
                .precioVenta(new BigDecimal("3500"))
                .receta(receta)
                .activo(true)
                .build();
    }

    @Test
    void productoSinRecetaNoSePuedeProducir() {
        Producto producto = Producto.builder()
                .id(5L)
                .nombre("Gaseosa")
                .stockActual(0)
                .precioVenta(new BigDecimal("2000"))
                .activo(true)
                .build();
        when(productoRepository.findById(5L)).thenReturn(Optional.of(producto));

        assertThrows(OperacionInvalidaException.class,
                () -> produccionService.registrar(new ProduccionRequest(5L, 1)));

        verify(produccionRepository, never()).save(any());
    }

    @Test
    void conInsumosSuficientesCompletaYAplicaEfectos() {
        Insumo harina = insumo(1L, "Harina", "1000", "0.0100");
        Producto producto = productoConReceta(harina, "100");
        when(productoRepository.findById(5L)).thenReturn(Optional.of(producto));
        when(insumoRepository.findByIdConLock(1L)).thenReturn(Optional.of(harina));
        when(productoRepository.findByIdConLock(5L)).thenReturn(Optional.of(producto));

        ProduccionResponse respuesta = produccionService.registrar(new ProduccionRequest(5L, 2));

        assertEquals(EstadoProduccion.COMPLETADA, respuesta.estado());
        assertEquals(0, respuesta.costoTotal().compareTo(new BigDecimal("2.00")));
        // 1000 g - (100 g x 2 unidades)
        assertEquals(0, harina.getStockActual().compareTo(new BigDecimal("800")));
        assertEquals(2, producto.getStockActual());

        ArgumentCaptor<Gasto> captor = ArgumentCaptor.forClass(Gasto.class);
        verify(gastoRepository).save(captor.capture());
        Gasto gasto = captor.getValue();
        assertEquals(CategoriaGasto.MATERIA_PRIMA, gasto.getCategoria());
        assertEquals(0, gasto.getMonto().compareTo(new BigDecimal("2.00")));
        assertEquals(1L, gasto.getProduccionId());
        verify(insumoRepository).save(harina);
    }

    @Test
    void conInsumosInsuficientesQuedaPendienteSinEfectos() {
        Insumo harina = insumo(1L, "Harina", "50", "0.0100");
        Producto producto = productoConReceta(harina, "100");
        when(productoRepository.findById(5L)).thenReturn(Optional.of(producto));
        when(insumoRepository.findByIdConLock(1L)).thenReturn(Optional.of(harina));

        ProduccionResponse respuesta = produccionService.registrar(new ProduccionRequest(5L, 2));

        assertEquals(EstadoProduccion.PENDIENTE, respuesta.estado());
        assertEquals(0, harina.getStockActual().compareTo(new BigDecimal("50")));
        assertEquals(0, producto.getStockActual());
        verify(insumoRepository, never()).save(any());
        verify(productoRepository, never()).save(any());
        verify(gastoRepository, never()).save(any());
    }
}