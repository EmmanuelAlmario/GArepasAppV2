package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.request.DetalleRecetaRequest;
import com.garepas.garepasapp.dto.request.RecetaRequest;
import com.garepas.garepasapp.dto.response.RecetaResponse;
import com.garepas.garepasapp.entity.Insumo;
import com.garepas.garepasapp.enums.UnidadMedida;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.repository.InsumoRepository;
import com.garepas.garepasapp.repository.ProductoRepository;
import com.garepas.garepasapp.repository.RecetaRepository;
import com.garepas.garepasapp.entity.Receta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifica el costo automatico de un insumo fabricado:
 * precio por gramo = Σ(cantidad × precio ingrediente) / rendimiento.
 */
class RecetaServiceTest {

    private RecetaRepository recetaRepository;
    private InsumoRepository insumoRepository;
    private RecetaService recetaService;

    private Insumo maiz;
    private Insumo sal;
    private Insumo masa;

    @BeforeEach
    void setUp() {
        recetaRepository = mock(RecetaRepository.class);
        insumoRepository = mock(InsumoRepository.class);
        recetaService = new RecetaService(recetaRepository, insumoRepository, mock(ProductoRepository.class));

        maiz = insumo(1L, "Maiz", "2.5");   // $2.5 por gramo
        sal = insumo(2L, "Sal", "1");       // $1 por gramo
        masa = insumo(3L, "Masa Preparada", "0");

        when(recetaRepository.save(any(Receta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(insumoRepository.findAllById(any())).thenReturn(List.of(maiz, sal));
        when(insumoRepository.findById(3L)).thenReturn(Optional.of(masa));
        when(recetaRepository.findByInsumoProducidoId(any())).thenReturn(List.of());
    }

    private Insumo insumo(Long id, String nombre, String precio) {
        return Insumo.builder()
                .id(id)
                .nombre(nombre)
                .categoria("Granos")
                .marca("Generica")
                .stockActual(new BigDecimal("1000"))
                .precioPorGramo(new BigDecimal(precio))
                .unidadMedida(UnidadMedida.GRAMO)
                .activo(true)
                .build();
    }

    private RecetaRequest masaRequest(Long insumoProducidoId, String rendimiento) {
        return new RecetaRequest(
                "Masa",
                null,
                List.of(
                        new DetalleRecetaRequest(1L, new BigDecimal("100"), UnidadMedida.GRAMO),
                        new DetalleRecetaRequest(2L, new BigDecimal("10"), UnidadMedida.GRAMO)),
                insumoProducidoId,
                rendimiento == null ? null : new BigDecimal(rendimiento));
    }

    @Test
    void precioDelInsumoFabricadoEsCostoDividido_entre_rendimiento() {
        // costo = 100 x 2.5 + 10 x 1 = 260 ; rendimiento 200 g -> 1.30 por gramo
        RecetaResponse respuesta = recetaService.crear(masaRequest(3L, "200"));

        assertEquals(0, new BigDecimal("1.300000").compareTo(masa.getPrecioPorGramo()));
        assertEquals(3L, respuesta.insumoProducidoId());
    }

    @Test
    void alCambiarIngredienteSeRecalculaElPrecioDelFabricado() {
        recetaService.crear(masaRequest(3L, "200"));
        // El maiz sube a $5/g: costo = 100 x 5 + 10 x 1 = 510 ; 510 / 200 = 2.55
        maiz.setPrecioPorGramo(new BigDecimal("5"));
        Receta receta = Receta.builder().id(9L).nombre("Masa").insumoProducido(masa)
                .rendimiento(new BigDecimal("200"))
                .detalles(new java.util.ArrayList<>(List.of(
                        com.garepas.garepasapp.entity.DetalleReceta.builder()
                                .insumo(maiz).cantidad(new BigDecimal("100")).unidadMedida(UnidadMedida.GRAMO).build(),
                        com.garepas.garepasapp.entity.DetalleReceta.builder()
                                .insumo(sal).cantidad(new BigDecimal("10")).unidadMedida(UnidadMedida.GRAMO).build())))
                .build();
        when(recetaRepository.findByInsumoProducidoIsNotNull()).thenReturn(List.of(receta));

        recetaService.recalcularInsumosProducidos();

        assertEquals(0, new BigDecimal("2.550000").compareTo(masa.getPrecioPorGramo()));
    }

    @Test
    void rendimientoCeroOSinValorEsInvalidoParaInsumoFabricado() {
        assertThrows(OperacionInvalidaException.class, () -> recetaService.crear(masaRequest(3L, "0")));
        assertThrows(OperacionInvalidaException.class, () -> recetaService.crear(masaRequest(3L, null)));
    }

    @Test
    void unInsumoFabricadoNoPuedeSerIngredienteDeSuPropiaReceta() {
        RecetaRequest autoreferencia = new RecetaRequest(
                "Masa",
                null,
                List.of(new DetalleRecetaRequest(3L, new BigDecimal("50"), UnidadMedida.GRAMO)),
                3L,
                new BigDecimal("100"));
        when(insumoRepository.findAllById(any())).thenReturn(List.of(masa));

        assertThrows(OperacionInvalidaException.class, () -> recetaService.crear(autoreferencia));
    }

    @Test
    void recetaDeProductoNoCalculaPrecioDeInsumo() {
        RecetaResponse respuesta = recetaService.crear(masaRequest(null, null));

        assertEquals(null, respuesta.insumoProducidoId());
        assertEquals(new BigDecimal("0"), masa.getPrecioPorGramo());
    }
}
