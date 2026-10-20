package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.ProduccionCommand;
import com.garepas.garepasapp.domain.enums.CategoriaGasto;
import com.garepas.garepasapp.domain.enums.EstadoProduccion;
import com.garepas.garepasapp.domain.enums.UnidadMedida;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.model.DetalleReceta;
import com.garepas.garepasapp.domain.model.Gasto;
import com.garepas.garepasapp.domain.model.Insumo;
import com.garepas.garepasapp.domain.model.Produccion;
import com.garepas.garepasapp.domain.model.Producto;
import com.garepas.garepasapp.domain.model.Receta;
import com.garepas.garepasapp.domain.repository.AjusteInventarioRepository;
import com.garepas.garepasapp.domain.repository.GastoRepository;
import com.garepas.garepasapp.domain.repository.InsumoRepository;
import com.garepas.garepasapp.domain.repository.ProduccionRepository;
import com.garepas.garepasapp.domain.repository.ProductoRepository;
import com.garepas.garepasapp.web.view.ProduccionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Produccion: consumo de insumos, gasto de materia prima, produccion
 * pendiente y reversion simetrica.
 */
class ProduccionServiceTest {

    private ProduccionRepository produccionRepository;
    private ProductoRepository productoRepository;
    private GastoRepository gastoRepository;
    private InsumoRepository insumoRepository;
    private ProduccionService produccionService;
    private final Map<Long, Insumo> insumos = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        produccionRepository = mock(ProduccionRepository.class);
        productoRepository = mock(ProductoRepository.class);
        gastoRepository = mock(GastoRepository.class);
        insumoRepository = mock(InsumoRepository.class);
        InventarioService inventario = new InventarioService(insumoRepository, productoRepository,
                mock(AjusteInventarioRepository.class));
        produccionService = new ProduccionService(produccionRepository, productoRepository,
                gastoRepository, inventario);

        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> {
            Produccion p = inv.getArgument(0);
            p.setId(11L);
            return p;
        });
    }

    private Insumo insumo(Long id, String nombre, String stock, String precioBase) {
        Insumo insumo = Insumo.builder()
                .id(id)
                .nombre(nombre)
                .categoria("Granos")
                .marca("Generica")
                .stockActual(new BigDecimal(stock))
                .precioPorUnidadBase(new BigDecimal(precioBase))
                .unidadPresentacion(UnidadMedida.GRAMO)
                .activo(true)
                .build();
        insumos.put(id, insumo);
        when(insumoRepository.findByIdConLock(id)).thenReturn(Optional.of(insumo));
        when(insumoRepository.findAllByIdConLock(any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) inv.getArgument(0);
            return ids.stream().map(insumos::get).filter(java.util.Objects::nonNull).toList();
        });
        return insumo;
    }

    private Producto productoConReceta(Receta receta) {
        Producto producto = Producto.builder()
                .id(1L)
                .nombre("Arepa")
                .stockActual(0)
                .precioVenta(new BigDecimal("3500"))
                .receta(receta)
                .activo(true)
                .build();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.findByIdConLock(1L)).thenReturn(Optional.of(producto));
        return producto;
    }

    private Receta receta(DetalleReceta... detalles) {
        Receta receta = Receta.builder().id(3L).nombre("Arepa").rendimiento(BigDecimal.ONE).build();
        for (DetalleReceta d : detalles) {
            d.setReceta(receta);
            receta.getDetalles().add(d);
        }
        return receta;
    }

    @Test
    void completaLaProduccionDescuentaStockYGeneraElGastoDeMateriaPrima() {
        Insumo harina = insumo(1L, "Harina", "1000", "2.00");
        Producto producto = productoConReceta(receta(
                DetalleReceta.builder().insumo(harina).cantidad(new BigDecimal("100")).build()));

        ProduccionResponse respuesta = produccionService.registrar(
                new ProduccionCommand(1L, 2));

        assertEquals(EstadoProduccion.COMPLETADA, respuesta.estado());
        assertEquals(0, new BigDecimal("800").compareTo(harina.getStockActual()));
        assertEquals(2, producto.getStockActual());
        assertEquals(0, new BigDecimal("400").compareTo(respuesta.costoTotal()));

        ArgumentCaptor<Gasto> captor = ArgumentCaptor.forClass(Gasto.class);
        verify(gastoRepository).save(captor.capture());
        assertEquals(CategoriaGasto.MATERIA_PRIMA, captor.getValue().getCategoria());
        assertEquals(0, new BigDecimal("400").compareTo(captor.getValue().getMonto()));
        assertEquals(11L, captor.getValue().getProduccion().getId());
    }

    @Test
    void dejaLaProduccionPendienteSiFaltanInsumos() {
        Insumo harina = insumo(1L, "Harina", "50", "2.00");
        productoConReceta(receta(
                DetalleReceta.builder().insumo(harina).cantidad(new BigDecimal("100")).build()));

        ProduccionResponse respuesta = produccionService.registrar(
                new ProduccionCommand(1L, 2));

        assertEquals(EstadoProduccion.PENDIENTE, respuesta.estado());
        assertEquals(0, new BigDecimal("50").compareTo(harina.getStockActual()),
                "Una produccion pendiente no descuenta stock");
        verify(gastoRepository, never()).save(any());
    }

    /**
     * REGRESION: antes la suficiencia se evaluaba fila por fila. Un insumo
     * repetido en dos filas (60 g + 60 g con 100 g de stock) pasaba las dos
     * como suficiente y el descuento dejaba el stock en -20. Ahora los
     * ingredientes se consolidan por insumo antes de calcular.
     */
    @Test
    void consolidaLosIngredientesRepetidosAlCalcularLaSuficiencia() {
        Insumo harina = insumo(1L, "Harina", "100", "2.00");
        productoConReceta(receta(
                DetalleReceta.builder().insumo(harina).cantidad(new BigDecimal("60")).build(),
                DetalleReceta.builder().insumo(harina).cantidad(new BigDecimal("60")).build()));

        ProduccionResponse respuesta = produccionService.registrar(
                new ProduccionCommand(1L, 1));

        assertEquals(EstadoProduccion.PENDIENTE, respuesta.estado(),
                "100 g de stock no cubren 120 g requeridos");
        assertEquals(1, respuesta.detalles().size(), "El insumo repetido queda en una sola linea");
        assertEquals(0, new BigDecimal("120").compareTo(respuesta.detalles().get(0).cantidadRequerida()));
        assertEquals(0, new BigDecimal("100").compareTo(harina.getStockActual()));
    }

    @Test
    void revertirLaProduccionDevuelveExactamenteLoDescontado() {
        Insumo harina = insumo(1L, "Harina", "1000", "2.00");
        Producto producto = productoConReceta(receta(
                DetalleReceta.builder().insumo(harina).cantidad(new BigDecimal("100")).build()));

        produccionService.registrar(new ProduccionCommand(1L, 2));
        assertEquals(0, new BigDecimal("800").compareTo(harina.getStockActual()));

        Produccion produccion = new Produccion();
        produccion.setId(11L);
        produccion.setProducto(producto);
        produccion.setCantidad(2);
        produccion.setEstado(EstadoProduccion.COMPLETADA);
        produccion.getDetalles().add(com.garepas.garepasapp.domain.model.DetalleProduccion.builder()
                .produccion(produccion)
                .insumo(harina)
                .cantidadRequerida(new BigDecimal("200"))
                .cantidadUsada(new BigDecimal("200"))
                .precioUnidadBaseSnapshot(new BigDecimal("2.00"))
                .costoLinea(new BigDecimal("400"))
                .suficiente(true)
                .build());
        when(produccionRepository.findById(11L)).thenReturn(Optional.of(produccion));

        produccionService.eliminar(11L);

        assertEquals(0, new BigDecimal("1000").compareTo(harina.getStockActual()),
                "La reversion devuelve el stock descontado");
        assertEquals(0, producto.getStockActual(), "La reversion tambien devuelve el stock del producto");
        verify(gastoRepository).deleteByProduccionId(11L);
    }

    @Test
    void rechazaProduccionDeUnProductoSinReceta() {
        Producto producto = Producto.builder().id(1L).nombre("Gaseosa").stockActual(5)
                .precioVenta(new BigDecimal("2000")).activo(true).build();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThrows(OperacionInvalidaException.class, () -> produccionService.registrar(
                new ProduccionCommand(1L, 1)));
    }
}
