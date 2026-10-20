package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.AjusteStockCommand;
import com.garepas.garepasapp.domain.command.DetalleAjusteStockCommand;
import com.garepas.garepasapp.domain.command.InsumoCommand;
import com.garepas.garepasapp.domain.enums.UnidadMedida;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.model.AjusteInventario;
import com.garepas.garepasapp.domain.model.Insumo;
import com.garepas.garepasapp.domain.repository.AjusteInventarioRepository;
import com.garepas.garepasapp.domain.repository.InsumoRepository;
import com.garepas.garepasapp.domain.repository.ProductoRepository;
import com.garepas.garepasapp.web.view.AjusteStockResponse;
import com.garepas.garepasapp.web.view.InsumoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Inventario de insumos: ajuste bidireccional, conversion de unidades y
 * bitacora de movimientos.
 */
class InsumoServiceTest {

    private InsumoRepository insumoRepository;
    private AjusteInventarioRepository ajusteRepository;
    private InsumoService insumoService;

    @BeforeEach
    void setUp() {
        insumoRepository = mock(InsumoRepository.class);
        ajusteRepository = mock(AjusteInventarioRepository.class);
        InventarioService inventario = new InventarioService(insumoRepository,
                mock(ProductoRepository.class), ajusteRepository);
        insumoService = new InsumoService(insumoRepository, mock(RecetaService.class), inventario);
    }

    private Insumo insumo(Long id, String nombre, String stock) {
        return insumo(id, nombre, stock, UnidadMedida.GRAMO);
    }

    private Insumo insumo(Long id, String nombre, String stock, UnidadMedida presentacion) {
        return Insumo.builder()
                .id(id)
                .nombre(nombre)
                .categoria("Granos")
                .marca("Generica")
                .stockActual(new BigDecimal(stock))
                .precioPorUnidadBase(new BigDecimal("0.0100"))
                .unidadPresentacion(presentacion)
                .activo(true)
                .build();
    }

    @Test
    void ajustaStockPositivoYNegativoEnUnaTransaccion() {
        Insumo harina = insumo(1L, "Harina", "100");
        Insumo queso = insumo(2L, "Queso", "50");
        when(insumoRepository.findAllByIdConLock(any())).thenReturn(List.of(harina, queso));

        AjusteStockResponse respuesta = insumoService.ajustarStock(new AjusteStockCommand(List.of(
                new DetalleAjusteStockCommand(1L, new BigDecimal("50")),
                new DetalleAjusteStockCommand(2L, new BigDecimal("-20")))), "admin");

        assertEquals(0, new BigDecimal("150").compareTo(harina.getStockActual()));
        assertEquals(0, new BigDecimal("30").compareTo(queso.getStockActual()));
        assertEquals(2, respuesta.cantidadInsumosAjustados());
    }

    @Test
    void rechazaAjusteQueDejariaStockNegativo() {
        Insumo harina = insumo(1L, "Harina", "10");
        when(insumoRepository.findAllByIdConLock(any())).thenReturn(List.of(harina));

        assertThrows(OperacionInvalidaException.class, () -> insumoService.ajustarStock(
                new AjusteStockCommand(List.of(
                        new DetalleAjusteStockCommand(1L, new BigDecimal("-20")))), "admin"));

        assertEquals(0, new BigDecimal("10").compareTo(harina.getStockActual()));
        verify(insumoRepository, never()).save(any());
        verify(ajusteRepository, never()).save(any());
    }

    @Test
    void rechazaCantidadCero() {
        Insumo harina = insumo(1L, "Harina", "10");
        when(insumoRepository.findAllByIdConLock(any())).thenReturn(List.of(harina));

        assertThrows(OperacionInvalidaException.class, () -> insumoService.ajustarStock(
                new AjusteStockCommand(List.of(
                        new DetalleAjusteStockCommand(1L, BigDecimal.ZERO))), "admin"));

        verify(insumoRepository, never()).save(any());
    }

    /**
     * REGRESION: el ajuste se captura en la unidad de PRESENTACION del insumo
     * y se convierte a unidad base en un solo punto (el servicio). Antes lo
     * hacia el formulario y el camino de la API no lo hacia.
     */
    @Test
    void convierteElAjusteDeKilogramosAGramos() {
        Insumo harina = insumo(1L, "Harina", "1000", UnidadMedida.KILOGRAMO);
        when(insumoRepository.findAllByIdConLock(any())).thenReturn(List.of(harina));

        insumoService.ajustarStock(new AjusteStockCommand(List.of(
                new DetalleAjusteStockCommand(1L, new BigDecimal("2")))), "admin");

        assertEquals(0, new BigDecimal("3000").compareTo(harina.getStockActual()),
                "2 kg de reponer sobre 1 kg deben dejar 3 kg (3000 g)");
    }

    @Test
    void dejaBitacoraDeCadaAjuste() {
        Insumo harina = insumo(1L, "Harina", "100");
        when(insumoRepository.findAllByIdConLock(any())).thenReturn(List.of(harina));

        insumoService.ajustarStock(new AjusteStockCommand(List.of(
                new DetalleAjusteStockCommand(1L, new BigDecimal("50")))), "cajero1");

        ArgumentCaptor<AjusteInventario> captor = ArgumentCaptor.forClass(AjusteInventario.class);
        verify(ajusteRepository).save(captor.capture());
        AjusteInventario asiento = captor.getValue();
        assertEquals("cajero1", asiento.getUsuario());
        assertEquals(0, new BigDecimal("50").compareTo(asiento.getDelta()));
        assertEquals(0, new BigDecimal("150").compareTo(asiento.getStockResultante()));
    }

    /**
     * REGRESION de unidades: al crear, el stock y el precio se capturan en la
     * unidad de presentacion y se GUARDAN en unidad base. La vista los vuelve
     * a mostrar en la unidad de presentacion.
     */
    @Test
    void alCrearConvierteStockYPrecioDePresentacionABase() {
        when(insumoRepository.existsByNombreIgnoreCase(any())).thenReturn(false);
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(inv -> inv.getArgument(0));

        InsumoResponse respuesta = insumoService.crear(new InsumoCommand(
                "Harina", "Granos", "Diana",
                new BigDecimal("5"), new BigDecimal("1"),
                new BigDecimal("4500"), UnidadMedida.KILOGRAMO, true));

        ArgumentCaptor<Insumo> captor = ArgumentCaptor.forClass(Insumo.class);
        verify(insumoRepository).save(captor.capture());
        Insumo guardada = captor.getValue();

        assertEquals(0, new BigDecimal("5000").compareTo(guardada.getStockActual()),
                "5 kg deben guardarse como 5000 g");
        assertEquals(0, new BigDecimal("4.500000").compareTo(guardada.getPrecioPorUnidadBase()),
                "4500 por kilo es 4.5 por gramo");
        assertEquals(UnidadMedida.KILOGRAMO, guardada.getUnidadPresentacion());

        // La vista lo muestra en la unidad de presentacion.
        assertEquals(0, new BigDecimal("5").compareTo(respuesta.stockActual()));
        assertEquals(0, new BigDecimal("4500").compareTo(respuesta.precioPorUnidad()));
        assertEquals(UnidadMedida.GRAMO, respuesta.unidadBase());
    }
}
