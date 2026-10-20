package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.DetalleRecetaCommand;
import com.garepas.garepasapp.domain.command.RecetaCommand;
import com.garepas.garepasapp.domain.enums.UnidadMedida;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.model.DetalleReceta;
import com.garepas.garepasapp.domain.model.Insumo;
import com.garepas.garepasapp.domain.model.Receta;
import com.garepas.garepasapp.domain.repository.InsumoRepository;
import com.garepas.garepasapp.domain.repository.ProductoRepository;
import com.garepas.garepasapp.domain.repository.RecetaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

  



class RecetaServiceTest {

    private RecetaRepository recetaRepository;
    private InsumoRepository insumoRepository;
    private RecetaService recetaService;

    @BeforeEach
    void setUp() {
        recetaRepository = mock(RecetaRepository.class);
        insumoRepository = mock(InsumoRepository.class);
        recetaService = new RecetaService(recetaRepository, insumoRepository, mock(ProductoRepository.class));
        when(recetaRepository.existsByNombreIgnoreCase(any())).thenReturn(false);
        when(recetaRepository.save(any(Receta.class))).thenAnswer(inv -> {
            Receta receta = inv.getArgument(0);
            receta.setId(5L);
            return receta;
        });
    }

    private Insumo insumo(Long id, String nombre, String precioBase, UnidadMedida presentacion) {
        Insumo insumo = Insumo.builder()
                .id(id)
                .nombre(nombre)
                .categoria("Granos")
                .marca("Generica")
                .stockActual(new BigDecimal("10000"))
                .precioPorUnidadBase(new BigDecimal(precioBase))
                .unidadPresentacion(presentacion)
                .activo(true)
                .build();
        when(insumoRepository.findById(id)).thenReturn(java.util.Optional.of(insumo));
        return insumo;
    }

      





    @Test
    void convierteLaCantidadDeKilogramosAGramos() {
        insumo(1L, "Harina", "4.5", UnidadMedida.KILOGRAMO);

        recetaService.crear(new RecetaCommand("Arepa", null,
                List.of(new DetalleRecetaCommand(1L, new BigDecimal("1"), UnidadMedida.KILOGRAMO)),
                null, null));

        ArgumentCaptor<Receta> captor = ArgumentCaptor.forClass(Receta.class);
        verify(recetaRepository).save(captor.capture());
        DetalleReceta detalle = captor.getValue().getDetalles().get(0);
        assertEquals(0, new BigDecimal("1000").compareTo(detalle.getCantidad()),
                "1 kg de harina son 1000 g");
    }

    @Test
    void consolidaElMismoInsumoRepetidoEnUnaSolaLinea() {
        insumo(1L, "Harina", "4.5", UnidadMedida.GRAMO);

        recetaService.crear(new RecetaCommand("Arepa", null, List.of(
                new DetalleRecetaCommand(1L, new BigDecimal("60"), UnidadMedida.GRAMO),
                new DetalleRecetaCommand(1L, new BigDecimal("40"), UnidadMedida.GRAMO)),
                null, null));

        ArgumentCaptor<Receta> captor = ArgumentCaptor.forClass(Receta.class);
        verify(recetaRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getDetalles().size());
        assertEquals(0, new BigDecimal("100").compareTo(captor.getValue().getDetalles().get(0).getCantidad()));
    }

    @Test
    void calculaElPrecioDelInsumoFabricadoComoCostoSobreRendimiento() {
        insumo(1L, "Harina", "2.50", UnidadMedida.GRAMO);
        Insumo masa = insumo(2L, "Masa preparada", "1.00", UnidadMedida.GRAMO);
        when(insumoRepository.findById(2L)).thenReturn(java.util.Optional.of(masa));

        recetaService.crear(new RecetaCommand("Masa", null,
                List.of(new DetalleRecetaCommand(1L, new BigDecimal("200"), UnidadMedida.GRAMO)),
                2L, new BigDecimal("100")));

          
        assertEquals(0, new BigDecimal("5.000000").compareTo(masa.getPrecioPorUnidadBase()));
    }

    @Test
    void exigeRendimientoCuandoLaRecetaFabricaUnInsumo() {
        insumo(1L, "Harina", "2.50", UnidadMedida.GRAMO);
        insumo(2L, "Masa preparada", "1.00", UnidadMedida.GRAMO);

        assertThrows(OperacionInvalidaException.class, () -> recetaService.crear(
                new RecetaCommand("Masa", null,
                        List.of(new DetalleRecetaCommand(1L, new BigDecimal("200"), UnidadMedida.GRAMO)),
                        2L, null)));
    }

    @Test
    void unInsumoNoPuedeSerIngredienteDeLaRecetaQueLoFabrica() {
        Insumo masa = insumo(2L, "Masa preparada", "1.00", UnidadMedida.GRAMO);

        assertThrows(OperacionInvalidaException.class, () -> recetaService.crear(
                new RecetaCommand("Masa", null,
                        List.of(new DetalleRecetaCommand(2L, new BigDecimal("200"), UnidadMedida.GRAMO)),
                        2L, new BigDecimal("100"))));
    }

    @Test
    void rechazaUnidadIncompatibleConElInsumo() {
        insumo(1L, "Harina", "4.5", UnidadMedida.KILOGRAMO);

        assertThrows(OperacionInvalidaException.class, () -> recetaService.crear(
                new RecetaCommand("Arepa", null,
                        List.of(new DetalleRecetaCommand(1L, new BigDecimal("100"), UnidadMedida.MILILITRO)),
                        null, null)));
    }

    @Test
    void rechazaRecetaSinIngredientes() {
        assertThrows(OperacionInvalidaException.class, () -> recetaService.crear(
                new RecetaCommand("Vacia", null, new ArrayList<>(), null, null)));
    }

}
