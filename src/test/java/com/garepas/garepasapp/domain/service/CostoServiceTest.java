package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.model.DetalleReceta;
import com.garepas.garepasapp.domain.model.Insumo;
import com.garepas.garepasapp.domain.model.Producto;
import com.garepas.garepasapp.domain.model.Receta;
import com.garepas.garepasapp.domain.repository.ProductoRepository;
import com.garepas.garepasapp.domain.repository.RecetaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

  



class CostoServiceTest {

    private RecetaRepository recetaRepository;
    private ProductoRepository productoRepository;
    private CostoService costoService;

    @BeforeEach
    void setUp() {
        recetaRepository = mock(RecetaRepository.class);
        productoRepository = mock(ProductoRepository.class);
        costoService = new CostoService(recetaRepository, productoRepository);
    }

    private Insumo insumo(Long id, String nombre, String precioBase) {
        return Insumo.builder()
                .id(id)
                .nombre(nombre)
                .stockActual(new BigDecimal("10000"))
                .precioPorUnidadBase(new BigDecimal(precioBase))
                .unidadPresentacion(com.garepas.garepasapp.domain.enums.UnidadMedida.GRAMO)
                .activo(true)
                .build();
    }

    private Receta receta(String nombre, BigDecimal rendimiento, DetalleReceta... detalles) {
        Receta receta = Receta.builder().id(1L).nombre(nombre).rendimiento(rendimiento).build();
        for (DetalleReceta d : detalles) {
            d.setReceta(receta);
            receta.getDetalles().add(d);
        }
        return receta;
    }

    @Test
    void elCostoDeLaRecetaSumaCantidadPorPrecioDeUnidadBase() {
        Receta receta = receta("Arepa", BigDecimal.ONE,
                DetalleReceta.builder().insumo(insumo(1L, "Harina", "4.50")).cantidad(new BigDecimal("120")).build(),
                DetalleReceta.builder().insumo(insumo(2L, "Queso", "14")).cantidad(new BigDecimal("40")).build());

        BigDecimal costo = CostoService.costoDeReceta(receta);

          
        assertEquals(0, new BigDecimal("1100").compareTo(costo));
    }

      




    @Test
    void elCostoUnitarioDividePorElRendimientoDeLaReceta() {
        Receta receta = receta("Lote", new BigDecimal("10"),
                DetalleReceta.builder().insumo(insumo(1L, "Harina", "5")).cantidad(new BigDecimal("1000")).build());
        Producto producto = Producto.builder().id(1L).nombre("Arepa").precioVenta(new BigDecimal("3500"))
                .receta(receta).stockActual(0).activo(true).build();

        BigDecimal costoUnitario = CostoService.costoUnitarioProducto(producto);

        assertEquals(0, new BigDecimal("500").compareTo(costoUnitario),
                "1000 g a 5 el gramo son 5000; rindiendo 10 unidades, 500 por unidad");
    }

    @Test
    void sinRendimientoInformadoSeAsumeUnaUnidad() {
        Receta receta = receta("Arepa", null,
                DetalleReceta.builder().insumo(insumo(1L, "Harina", "5")).cantidad(new BigDecimal("100")).build());
        Producto producto = Producto.builder().id(1L).nombre("Arepa").precioVenta(new BigDecimal("3500"))
                .receta(receta).stockActual(0).activo(true).build();

        assertEquals(0, new BigDecimal("500").compareTo(CostoService.costoUnitarioProducto(producto)));
    }

    @Test
    void unProductoSinRecetaNoTieneCosto() {
        Producto producto = Producto.builder().id(1L).nombre("Gaseosa").precioVenta(new BigDecimal("2000"))
                .stockActual(5).activo(true).build();

        assertEquals(0, BigDecimal.ZERO.compareTo(CostoService.costoUnitarioProducto(producto)));
    }

      




    @Test
    void elMargenDeSugerenciaSeInterpretaComoPorcentaje() {
        Producto producto = Producto.builder().id(1L).nombre("Arepa").precioVenta(new BigDecimal("3500"))
                .stockActual(0).activo(true).build();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        BigDecimal sugerido = costoService.sugerirPrecioVenta(1L, new BigDecimal("30"));

        assertEquals(0, new BigDecimal("0").compareTo(sugerido));
    }

    @Test
    void elMargenDeSugerenciaAplicaElPorcentajeSobreElCosto() {
        Receta receta = receta("Arepa", BigDecimal.ONE,
                DetalleReceta.builder().insumo(insumo(1L, "Harina", "5")).cantidad(new BigDecimal("100")).build());
        Producto producto = Producto.builder().id(1L).nombre("Arepa").precioVenta(new BigDecimal("3500"))
                .receta(receta).stockActual(0).activo(true).build();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

          
        BigDecimal sugerido = costoService.sugerirPrecioVenta(1L, new BigDecimal("30"));

        assertEquals(0, new BigDecimal("650.00").compareTo(sugerido));
    }

    @Test
    void rechazaUnMargenNegativo() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(
                Producto.builder().id(1L).nombre("Arepa").precioVenta(new BigDecimal("3500")).build()));

        assertThrows(com.garepas.garepasapp.domain.exception.OperacionInvalidaException.class,
                () -> costoService.sugerirPrecioVenta(1L, new BigDecimal("-10")));
    }
}
