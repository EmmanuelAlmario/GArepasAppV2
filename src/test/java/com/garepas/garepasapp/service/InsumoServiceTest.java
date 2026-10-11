package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.request.AjusteStockRequest;
import com.garepas.garepasapp.dto.request.DetalleAjusteStockRequest;
import com.garepas.garepasapp.dto.response.AjusteStockResponse;
import com.garepas.garepasapp.entity.Insumo;
import com.garepas.garepasapp.enums.UnidadMedida;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.repository.InsumoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifica la regla de ajuste de stock bidireccional:
 * delta positivo repone, delta negativo consume, y nunca queda stock negativo.
 */
class InsumoServiceTest {

    private InsumoRepository insumoRepository;
    private InsumoService insumoService;

    @BeforeEach
    void setUp() {
        insumoRepository = mock(InsumoRepository.class);
        insumoService = new InsumoService(insumoRepository, mock(RecetaService.class));
    }

    private Insumo insumo(Long id, String nombre, String stock) {
        return Insumo.builder()
                .id(id)
                .nombre(nombre)
                .categoria("Granos")
                .marca("Genérica")
                .stockActual(new BigDecimal(stock))
                .precioPorGramo(new BigDecimal("0.0100"))
                .unidadMedida(UnidadMedida.GRAMO)
                .activo(true)
                .build();
    }

    @Test
    void ajustaStockPositivoYNegativoEnUnaTransaccion() {
        Insumo harina = insumo(1L, "Harina", "100");
        Insumo queso = insumo(2L, "Queso", "50");
        when(insumoRepository.findAllById(any())).thenReturn(List.of(harina, queso));

        AjusteStockResponse respuesta = insumoService.ajustarStock(new AjusteStockRequest(List.of(
                new DetalleAjusteStockRequest(1L, new BigDecimal("50")),
                new DetalleAjusteStockRequest(2L, new BigDecimal("-20")))));

        assertEquals(new BigDecimal("150"), harina.getStockActual());
        assertEquals(new BigDecimal("30"), queso.getStockActual());
        assertEquals(2, respuesta.cantidadInsumosAjustados());
        verify(insumoRepository, times(2)).save(any(Insumo.class));
    }

    @Test
    void rechazaAjusteQueDejariaStockNegativo() {
        Insumo harina = insumo(1L, "Harina", "10");
        when(insumoRepository.findAllById(any())).thenReturn(List.of(harina));

        assertThrows(OperacionInvalidaException.class, () -> insumoService.ajustarStock(
                new AjusteStockRequest(List.of(
                        new DetalleAjusteStockRequest(1L, new BigDecimal("-20"))))));

        assertEquals(new BigDecimal("10"), harina.getStockActual());
        verify(insumoRepository, never()).save(any());
    }

    @Test
    void rechazaCantidadCero() {
        Insumo harina = insumo(1L, "Harina", "10");
        when(insumoRepository.findAllById(any())).thenReturn(List.of(harina));

        assertThrows(OperacionInvalidaException.class, () -> insumoService.ajustarStock(
                new AjusteStockRequest(List.of(
                        new DetalleAjusteStockRequest(1L, BigDecimal.ZERO)))));

        verify(insumoRepository, never()).save(any());
    }
}