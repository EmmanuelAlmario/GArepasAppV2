package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.AbonoCommand;
import com.garepas.garepasapp.domain.command.ClienteCommand;
import com.garepas.garepasapp.domain.command.VentaCommand;
import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.enums.FormaPago;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.domain.model.Abono;
import com.garepas.garepasapp.domain.model.Cliente;
import com.garepas.garepasapp.domain.model.ConfiguracionNegocio;
import com.garepas.garepasapp.domain.model.Producto;
import com.garepas.garepasapp.domain.model.Venta;
import com.garepas.garepasapp.domain.repository.AbonoRepository;
import com.garepas.garepasapp.domain.repository.ClienteRepository;
import com.garepas.garepasapp.domain.repository.ConfiguracionNegocioRepository;
import com.garepas.garepasapp.domain.repository.InsumoRepository;
import com.garepas.garepasapp.domain.repository.JornadaRepository;
import com.garepas.garepasapp.domain.repository.ProductoRepository;
import com.garepas.garepasapp.domain.repository.VentaRepository;
import com.garepas.garepasapp.domain.command.DetalleVentaCommand;
import com.garepas.garepasapp.web.view.VentaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Funcionalidad de Treinta aplicada al negocio: clientes, venta a credito,
 * abonos, cartera e impuestos.
 */
class CarteraTest {

    private VentaRepository ventaRepository;
    private ProductoRepository productoRepository;
    private JornadaRepository jornadaRepository;
    private ClienteRepository clienteRepository;
    private AbonoRepository abonoRepository;
    private ConfiguracionNegocioRepository configRepository;
    private VentaService ventaService;
    private AbonoService abonoService;
    private ClienteService clienteService;
    private ConfiguracionNegocioService configService;

    @BeforeEach
    void setUp() {
        ventaRepository = mock(VentaRepository.class);
        productoRepository = mock(ProductoRepository.class);
        jornadaRepository = mock(JornadaRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        abonoRepository = mock(AbonoRepository.class);
        configRepository = mock(ConfiguracionNegocioRepository.class);
        InsumoRepository insumoRepository = mock(InsumoRepository.class);

        InventarioService inventario = new InventarioService(insumoRepository, productoRepository,
                mock(com.garepas.garepasapp.domain.repository.AjusteInventarioRepository.class));
        configService = new ConfiguracionNegocioService(configRepository);
        ventaService = new VentaService(ventaRepository, jornadaRepository, clienteRepository,
                mock(ProduccionService.class), inventario, configService);
        abonoService = new AbonoService(abonoRepository, ventaRepository);
        clienteService = new ClienteService(clienteRepository, ventaRepository);

        when(jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc()).thenReturn(Optional.empty());
        when(configRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
        when(configRepository.save(any(ConfiguracionNegocio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId(99L);
            return v;
        });
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(7L);
            return c;
        });
    }

    private Producto producto(int stock, String precio) {
        Producto p = Producto.builder()
                .id(1L).nombre("Arepa").stockActual(stock)
                .precioVenta(new BigDecimal(precio)).activo(true).build();
        when(productoRepository.findAllByIdConLock(any())).thenReturn(List.of(p));
        return p;
    }

    private Cliente cliente(String nombre) {
        Cliente c = Cliente.builder().id(7L).nombre(nombre).activo(true).build();
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void unaVentaACreditoQuedaConSaldoYConVencimiento() {
        producto(10, "3500");
        cliente("Maria");

        VentaResponse venta = ventaService.registrar(new VentaCommand(null, 7L, FormaPago.CREDITO,
                LocalDate.now().plusDays(15),
                List.of(new DetalleVentaCommand(1L, 2))));

        assertEquals(0, new BigDecimal("7000").compareTo(venta.total()));
        assertEquals(0, new BigDecimal("7000").compareTo(venta.saldo()));
        assertTrue(venta.pendiente());
        assertEquals(FormaPago.CREDITO, venta.formaPago());
        assertEquals("Maria", venta.nombreCliente());
    }

    @Test
    void unaVentaACreditoSinClienteSeRechaza() {
        producto(10, "3500");

        assertThrows(OperacionInvalidaException.class, () -> ventaService.registrar(
                new VentaCommand(null, null, FormaPago.CREDITO, LocalDate.now().plusDays(10),
                        List.of(new DetalleVentaCommand(1L, 1)))));
    }

    @Test
    void unaVentaPagadaNoDejaSaldo() {
        producto(10, "3500");

        VentaResponse venta = ventaService.registrar(new VentaCommand("Consumidor", null,
                FormaPago.EFECTIVO, null, List.of(new DetalleVentaCommand(1L, 2))));

        assertEquals(0, BigDecimal.ZERO.compareTo(venta.saldo()));
        assertTrue(!venta.pendiente());
    }

    @Test
    void elImpuestoDelNegocioSeSumaAlTotal() {
        producto(10, "10000");
        when(configRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(
                ConfiguracionNegocio.builder().id(1L).nombreComercial("Gordo Arepas")
                        .impuestoPorcentaje(new BigDecimal("19")).build()));

        VentaResponse venta = ventaService.registrar(new VentaCommand(null, null, FormaPago.EFECTIVO,
                null, List.of(new DetalleVentaCommand(1L, 1))));

        assertEquals(0, new BigDecimal("10000").compareTo(venta.subtotal()));
        assertEquals(0, new BigDecimal("1900").compareTo(venta.impuestos()));
        assertEquals(0, new BigDecimal("11900").compareTo(venta.total()));
    }

    @Test
    void unAbonoBajaElSaldoDeLaVenta() {
        Venta venta = Venta.builder().id(99L).total(new BigDecimal("7000")).saldo(new BigDecimal("7000"))
                .estado(EstadoVenta.ACTIVA).formaPago(FormaPago.CREDITO).build();
        when(ventaRepository.findById(99L)).thenReturn(Optional.of(venta));
        when(abonoRepository.save(any(Abono.class))).thenAnswer(inv -> inv.getArgument(0));

        abonoService.registrar(99L, new AbonoCommand(new BigDecimal("2000"), FormaPago.EFECTIVO, "Abono"));

        assertEquals(0, new BigDecimal("5000").compareTo(venta.getSaldo()));
        verify(ventaRepository).save(venta);
    }

    @Test
    void noSePuedeAbonarMasQueElSaldoPendiente() {
        Venta venta = Venta.builder().id(99L).total(new BigDecimal("7000")).saldo(new BigDecimal("1000"))
                .estado(EstadoVenta.ACTIVA).formaPago(FormaPago.CREDITO).build();
        when(ventaRepository.findById(99L)).thenReturn(Optional.of(venta));

        assertThrows(OperacionInvalidaException.class,
                () -> abonoService.registrar(99L, new AbonoCommand(new BigDecimal("5000"), FormaPago.EFECTIVO, null)));
    }

    @Test
    void noSePuedeAbonarAVentaYaPagada() {
        Venta venta = Venta.builder().id(99L).total(new BigDecimal("7000")).saldo(BigDecimal.ZERO)
                .estado(EstadoVenta.ACTIVA).formaPago(FormaPago.CREDITO).build();
        when(ventaRepository.findById(99L)).thenReturn(Optional.of(venta));

        assertThrows(OperacionInvalidaException.class,
                () -> abonoService.registrar(99L, new AbonoCommand(new BigDecimal("1000"), FormaPago.EFECTIVO, null)));
    }

    @Test
    void noSePuedeAnularUnaVentaConSaldoPendiente() {
        Venta venta = Venta.builder().id(99L).total(new BigDecimal("7000")).saldo(new BigDecimal("7000"))
                .estado(EstadoVenta.ACTIVA).formaPago(FormaPago.CREDITO).build();
        when(ventaRepository.findById(99L)).thenReturn(Optional.of(venta));

        assertThrows(OperacionInvalidaException.class, () -> ventaService.anular(99L));
    }

    @Test
    void unClienteConVentasSeDesactivaEnLugarDeBorrarse() {
        Cliente c = Cliente.builder().id(7L).nombre("Maria").activo(true).build();
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(c));
        when(ventaRepository.countVentasPorCliente(7L)).thenReturn(3L);

        assertThrows(OperacionInvalidaException.class, () -> clienteService.eliminar(7L));

        assertTrue(!c.getActivo());
        verify(clienteRepository, never()).deleteById(any());
    }

    @Test
    void noSePuedeCrearDosClientesConElMismoNombre() {
        when(clienteRepository.existsByNombreIgnoreCase("Maria")).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class,
                () -> clienteService.crear(new ClienteCommand("Maria", null, null, null, true)));
    }
}
