package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.request.DetalleVentaRequest;
import com.garepas.garepasapp.dto.request.VentaRequest;
import com.garepas.garepasapp.dto.response.VentaResponse;
import com.garepas.garepasapp.entity.DetalleVenta;
import com.garepas.garepasapp.entity.Jornada;
import com.garepas.garepasapp.entity.Producto;
import com.garepas.garepasapp.entity.Venta;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.repository.JornadaRepository;
import com.garepas.garepasapp.repository.ProductoRepository;
import com.garepas.garepasapp.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Reglas del punto de venta: precio del servidor, descuento de stock,
 * rechazo de productos inactivos o sin stock, y reversión al eliminar.
 */
class VentaServiceTest {

    private VentaRepository ventaRepository;
    private ProductoRepository productoRepository;
    private JornadaRepository jornadaRepository;
    private VentaService ventaService;

    @BeforeEach
    void setUp() {
        ventaRepository = mock(VentaRepository.class);
        productoRepository = mock(ProductoRepository.class);
        jornadaRepository = mock(JornadaRepository.class);
        ventaService = new VentaService(ventaRepository, productoRepository, jornadaRepository);
        when(jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc()).thenReturn(Optional.empty());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta venta = inv.getArgument(0);
            venta.setId(99L);
            return venta;
        });
    }

    private Producto producto(Long id, String nombre, int stock, String precio, boolean activo) {
        return Producto.builder()
                .id(id)
                .nombre(nombre)
                .stockActual(stock)
                .precioVenta(new BigDecimal(precio))
                .activo(activo)
                .build();
    }

    @Test
    void registraVentaDescuentaStockYUsaPrecioDelServidor() {
        Producto arepa = producto(1L, "Arepa de queso", 10, "3500", true);
        when(productoRepository.findByIdConLock(1L)).thenReturn(Optional.of(arepa));

        VentaResponse respuesta = ventaService.registrar(
                new VentaRequest("Ana", List.of(new DetalleVentaRequest(1L, 2))));

        assertEquals(99L, respuesta.id());
        assertEquals(0, respuesta.total().compareTo(new BigDecimal("7000")));
        assertEquals(8, arepa.getStockActual());
        assertEquals(1, respuesta.detalles().size());
        assertEquals("Arepa de queso", respuesta.detalles().get(0).productoNombre());
        verify(productoRepository).save(arepa);
        verify(ventaRepository).save(any(Venta.class));
    }

    @Test
    void rechazaProductoInactivo() {
        Producto arepa = producto(1L, "Arepa", 10, "3500", false);
        when(productoRepository.findByIdConLock(1L)).thenReturn(Optional.of(arepa));

        assertThrows(OperacionInvalidaException.class, () -> ventaService.registrar(
                new VentaRequest(null, List.of(new DetalleVentaRequest(1L, 1)))));

        assertEquals(10, arepa.getStockActual());
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void rechazaStockInsuficiente() {
        Producto arepa = producto(1L, "Arepa", 1, "3500", true);
        when(productoRepository.findByIdConLock(1L)).thenReturn(Optional.of(arepa));

        assertThrows(OperacionInvalidaException.class, () -> ventaService.registrar(
                new VentaRequest(null, List.of(new DetalleVentaRequest(1L, 2)))));

        assertEquals(1, arepa.getStockActual());
        verify(productoRepository, never()).save(any());
    }

    @Test
    void rechazaVentaSinDetalles() {
        assertThrows(OperacionInvalidaException.class, () -> ventaService.registrar(
                new VentaRequest(null, List.of())));

        verifyNoInteractions(productoRepository, ventaRepository);
    }

    @Test
    void asociaLaJornadaActiva() {
        Producto arepa = producto(1L, "Arepa", 10, "3500", true);
        when(productoRepository.findByIdConLock(1L)).thenReturn(Optional.of(arepa));
        when(jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc())
                .thenReturn(Optional.of(Jornada.builder().id(7L).activa(true).build()));

        VentaResponse respuesta = ventaService.registrar(
                new VentaRequest(null, List.of(new DetalleVentaRequest(1L, 1))));

        assertEquals(7L, respuesta.jornadaId());
    }

    @Test
    void eliminarDevuelveElStockDelProducto() {
        Producto arepa = producto(1L, "Arepa", 5, "3500", true);
        Venta venta = Venta.builder()
                .id(50L)
                .fecha(LocalDateTime.now())
                .total(new BigDecimal("10500"))
                .build();
        venta.getDetalles().add(DetalleVenta.builder()
                .venta(venta)
                .producto(arepa)
                .cantidad(3)
                .precioUnitario(new BigDecimal("3500"))
                .subtotal(new BigDecimal("10500"))
                .build());
        when(ventaRepository.findById(50L)).thenReturn(Optional.of(venta));
        when(productoRepository.findByIdConLock(1L)).thenReturn(Optional.of(arepa));

        ventaService.eliminar(50L);

        assertEquals(8, arepa.getStockActual());
        verify(productoRepository).save(arepa);
        verify(ventaRepository).deleteById(50L);
    }
}