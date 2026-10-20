package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.DetalleVentaCommand;
import com.garepas.garepasapp.domain.enums.FormaPago;
import com.garepas.garepasapp.domain.command.VentaCommand;
import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.model.DetalleVenta;
import com.garepas.garepasapp.domain.model.ConfiguracionNegocio;
import com.garepas.garepasapp.domain.model.Jornada;
import com.garepas.garepasapp.domain.model.Producto;
import com.garepas.garepasapp.domain.model.Venta;
import com.garepas.garepasapp.domain.repository.AjusteInventarioRepository;
import com.garepas.garepasapp.domain.repository.InsumoRepository;
import com.garepas.garepasapp.domain.repository.AjusteInventarioRepository;
import com.garepas.garepasapp.domain.repository.ClienteRepository;
import com.garepas.garepasapp.domain.repository.ConfiguracionNegocioRepository;
import com.garepas.garepasapp.domain.repository.InsumoRepository;
import com.garepas.garepasapp.domain.repository.JornadaRepository;
import com.garepas.garepasapp.domain.repository.ProductoRepository;
import com.garepas.garepasapp.domain.repository.VentaRepository;
import com.garepas.garepasapp.web.view.VentaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Reglas del punto de venta: precio del servidor, consolidacion de lineas,
 * descuento de stock, rechazo de productos inactivos o sin stock, produccion
 * automatica para productos bajo pedido y anulacion con devolucion de stock.
 */
class VentaServiceTest {

    private VentaRepository ventaRepository;
    private ProductoRepository productoRepository;
    private JornadaRepository jornadaRepository;
    private ProduccionService produccionService;
    private InventarioService inventarioService;
    private VentaService ventaService;

    @BeforeEach
    void setUp() {
        ventaRepository = mock(VentaRepository.class);
        productoRepository = mock(ProductoRepository.class);
        jornadaRepository = mock(JornadaRepository.class);
        produccionService = mock(ProduccionService.class);
        inventarioService = new InventarioService(mock(InsumoRepository.class), productoRepository,
                mock(AjusteInventarioRepository.class));
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        ConfiguracionNegocioRepository configRepository = mock(ConfiguracionNegocioRepository.class);
        when(configRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(
                ConfiguracionNegocio.builder().id(1L).nombreComercial("Gordo Arepas")
                        .impuestoPorcentaje(BigDecimal.ZERO).build()));
        ConfiguracionNegocioService configuracion = new ConfiguracionNegocioService(configRepository);
        ventaService = new VentaService(ventaRepository, jornadaRepository, clienteRepository,
                produccionService, inventarioService, configuracion);

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

    /** El inventario bloquea los productos en una sola consulta (ordenada por id). */
    private void MockearProductos(Producto... productos) {
        when(productoRepository.findAllByIdConLock(any())).thenReturn(List.of(productos));
    }

    @Test
    void registraVentaDescuentaStockYUsaPrecioDelServidor() {
        Producto arepa = producto(1L, "Arepa de queso", 10, "3500", true);
        MockearProductos(arepa);

        VentaResponse respuesta = ventaService.registrar(
                new VentaCommand("Ana", null, FormaPago.EFECTIVO, null, List.of(new DetalleVentaCommand(1L, 2))));

        assertEquals(99L, respuesta.id());
        assertEquals(0, respuesta.total().compareTo(new BigDecimal("7000")));
        assertEquals(8, arepa.getStockActual());
        assertEquals(1, respuesta.detalles().size());
        assertEquals("Arepa de queso", respuesta.detalles().get(0).productoNombre());
        assertEquals(EstadoVenta.ACTIVA, respuesta.estado());
    }

    /**
     * REGRESION: antes la validacion de stock era linea por linea. Dos lineas
     * del mismo producto (5 + 5 con 8 disponibles) pasaban las dos y el
     * inventario quedaba en -2. Ahora las lineas se consolidan antes de
     * validar.
     */
    @Test
    void rechazaVentaConElMismoProductoRepetidoQueExcedeElStock() {
        Producto arepa = producto(1L, "Arepa", 8, "3500", true);
        MockearProductos(arepa);

        assertThrows(OperacionInvalidaException.class, () -> ventaService.registrar(
                new VentaCommand(null, null, FormaPago.EFECTIVO, null, List.of(
                        new DetalleVentaCommand(1L, 5),
                        new DetalleVentaCommand(1L, 5)))));

        assertEquals(8, arepa.getStockActual(), "El stock no debe tocarse cuando la venta se rechaza");
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void consolidaLineasDelMismoProductoYDescuentaElTotal() {
        Producto arepa = producto(1L, "Arepa", 10, "3500", true);
        MockearProductos(arepa);

        VentaResponse respuesta = ventaService.registrar(
                new VentaCommand(null, null, FormaPago.EFECTIVO, null, List.of(
                        new DetalleVentaCommand(1L, 3),
                        new DetalleVentaCommand(1L, 2))));

        assertEquals(5, arepa.getStockActual());
        assertEquals(0, respuesta.total().compareTo(new BigDecimal("17500")));
        assertEquals(2, respuesta.detalles().size(), "Se conservan las lineas originales del pedido");
    }

    @Test
    void rechazaProductoInactivo() {
        Producto arepa = producto(1L, "Arepa", 10, "3500", false);
        MockearProductos(arepa);

        assertThrows(OperacionInvalidaException.class, () -> ventaService.registrar(
                new VentaCommand(null, null, FormaPago.EFECTIVO, null, List.of(new DetalleVentaCommand(1L, 1)))));

        assertEquals(10, arepa.getStockActual());
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void rechazaStockInsuficiente() {
        Producto arepa = producto(1L, "Arepa", 1, "3500", true);
        MockearProductos(arepa);

        assertThrows(OperacionInvalidaException.class, () -> ventaService.registrar(
                new VentaCommand(null, null, FormaPago.EFECTIVO, null, List.of(new DetalleVentaCommand(1L, 2)))));

        assertEquals(1, arepa.getStockActual());
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void rechazaVentaSinDetalles() {
        assertThrows(OperacionInvalidaException.class, () -> ventaService.registrar(
                new VentaCommand(null, null, FormaPago.EFECTIVO, null, List.of())));

        verifyNoInteractions(productoRepository, ventaRepository);
    }

    @Test
    void rechazaCantidadNoPositiva() {
        assertThrows(OperacionInvalidaException.class, () -> ventaService.registrar(
                new VentaCommand(null, null, FormaPago.EFECTIVO, null, List.of(new DetalleVentaCommand(1L, 0)))));

        verifyNoInteractions(productoRepository, ventaRepository);
    }

    @Test
    void asociaLaJornadaActiva() {
        Producto arepa = producto(1L, "Arepa", 10, "3500", true);
        MockearProductos(arepa);
        when(jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc())
                .thenReturn(Optional.of(Jornada.builder().id(7L).activa(true).build()));

        VentaResponse respuesta = ventaService.registrar(
                new VentaCommand(null, null, FormaPago.EFECTIVO, null, List.of(new DetalleVentaCommand(1L, 1))));

        assertEquals(7L, respuesta.jornadaId());
    }

    @Test
    void productoBajoPedidoVendeSinStockYGeneraProduccionAutomatica() {
        Producto arepaDeCerdo = producto(1L, "Arepa de cerdo", 0, "6000", true);
        arepaDeCerdo.setBajoPedido(true);
        MockearProductos(arepaDeCerdo);

        VentaResponse respuesta = ventaService.registrar(
                new VentaCommand(null, null, FormaPago.EFECTIVO, null, List.of(new DetalleVentaCommand(1L, 2))));

        assertEquals(0, respuesta.total().compareTo(new BigDecimal("12000")));
        verify(produccionService).registrarParaVenta(eq(arepaDeCerdo), eq(2), any(Venta.class));
        assertEquals(0, arepaDeCerdo.getStockActual(), "El stock de producto bajo pedido queda inerte");
    }

    @Test
    void productoBajoPedidoPropagaElRechazoPorInsumosInsuficientes() {
        Producto arepaDeCerdo = producto(1L, "Arepa de cerdo", 0, "6000", true);
        arepaDeCerdo.setBajoPedido(true);
        MockearProductos(arepaDeCerdo);
        when(produccionService.registrarParaVenta(eq(arepaDeCerdo), eq(1), any(Venta.class)))
                .thenThrow(new OperacionInvalidaException("Insumos insuficientes para 'Arepa de cerdo'"));

        assertThrows(OperacionInvalidaException.class, () -> ventaService.registrar(
                new VentaCommand(null, null, FormaPago.EFECTIVO, null, List.of(new DetalleVentaCommand(1L, 1)))));
    }

    @Test
    void productoBajoPedidoNoSeValidaPorStock() {
        Producto arepaDeCerdo = producto(1L, "Arepa de cerdo", 0, "6000", true);
        arepaDeCerdo.setBajoPedido(true);
        MockearProductos(arepaDeCerdo);

        ventaService.registrar(new VentaCommand(null, null, FormaPago.EFECTIVO, null, List.of(new DetalleVentaCommand(1L, 5))));

        verify(produccionService).registrarParaVenta(eq(arepaDeCerdo), eq(5), any(Venta.class));
    }

    /**
     * La venta ya no se borra: se anula. El stock vuelve, las producciones
     * automaticas se revierten y la venta queda como historia.
     */
    @Test
    void anularDevuelveElStockYMarcaLaVentaComoAnulada() {
        Producto arepa = producto(1L, "Arepa", 5, "3500", true);
        Venta venta = Venta.builder()
                .id(50L)
                .fecha(LocalDateTime.now())
                .total(new BigDecimal("10500"))
                .estado(EstadoVenta.ACTIVA)
                .build();
        venta.getDetalles().add(DetalleVenta.builder()
                .venta(venta)
                .producto(arepa)
                .cantidad(3)
                .precioUnitario(new BigDecimal("3500"))
                .subtotal(new BigDecimal("10500"))
                .build());
        when(ventaRepository.findById(50L)).thenReturn(Optional.of(venta));
        MockearProductos(arepa);

        VentaResponse respuesta = ventaService.anular(50L);

        assertEquals(8, arepa.getStockActual());
        assertEquals(EstadoVenta.ANULADA, respuesta.estado());
        assertFalse(respuesta.activa());
        verify(ventaRepository, never()).deleteById(any());
    }

    @Test
    void anularDeVentaDeProductoBajoPedidoRevierteLaProduccion() {
        Producto arepaDeCerdo = producto(1L, "Arepa de cerdo", 0, "6000", true);
        arepaDeCerdo.setBajoPedido(true);
        Venta venta = Venta.builder()
                .id(50L)
                .fecha(LocalDateTime.now())
                .total(new BigDecimal("6000"))
                .estado(EstadoVenta.ACTIVA)
                .build();
        venta.getDetalles().add(DetalleVenta.builder()
                .venta(venta)
                .producto(arepaDeCerdo)
                .cantidad(1)
                .precioUnitario(new BigDecimal("6000"))
                .subtotal(new BigDecimal("6000"))
                .build());
        when(ventaRepository.findById(50L)).thenReturn(Optional.of(venta));
        MockearProductos(arepaDeCerdo);

        ventaService.anular(50L);

        assertEquals(0, arepaDeCerdo.getStockActual());
        verify(produccionService).revertirDeVenta(venta);
    }

    @Test
    void anularUnaVentaYaAnuladaFalla() {
        Venta venta = Venta.builder().id(50L).estado(EstadoVenta.ANULADA).build();
        when(ventaRepository.findById(50L)).thenReturn(Optional.of(venta));

        assertThrows(OperacionInvalidaException.class, () -> ventaService.anular(50L));
    }

    @Test
    void laVentaRegistradaQuedaActivaPorDefecto() {
        Producto arepa = producto(1L, "Arepa", 10, "3500", true);
        MockearProductos(arepa);

        VentaResponse respuesta = ventaService.registrar(
                new VentaCommand(null, null, FormaPago.EFECTIVO, null, List.of(new DetalleVentaCommand(1L, 1))));

        assertTrue(respuesta.activa());
    }
}
