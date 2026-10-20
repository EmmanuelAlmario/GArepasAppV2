package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.model.Jornada;
import com.garepas.garepasapp.domain.repository.JornadaRepository;
import com.garepas.garepasapp.domain.repository.VentaRepository;
import com.garepas.garepasapp.web.view.JornadaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Jornadas: apertura, cierre y arqueo. El arqueo se resuelve con una consulta
 * agregada (no cargando la tabla de ventas).
 */
class JornadaServiceTest {

    private JornadaRepository jornadaRepository;
    private VentaRepository ventaRepository;
    private JornadaService jornadaService;

    @BeforeEach
    void setUp() {
        jornadaRepository = mock(JornadaRepository.class);
        ventaRepository = mock(VentaRepository.class);
        jornadaService = new JornadaService(jornadaRepository, ventaRepository);
    }

    @Test
    void abrirCreaLaJornadaActivaConQuienLaAbre() {
        when(jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc()).thenReturn(Optional.empty());
        when(jornadaRepository.save(any(Jornada.class))).thenAnswer(inv -> {
            Jornada j = inv.getArgument(0);
            j.setId(1L);
            return j;
        });
        when(ventaRepository.arqueoPorJornada(any())).thenReturn(List.of());

        JornadaResponse jornada = jornadaService.abrir("admin");

        assertTrue(jornada.activa());
        assertEquals("admin", jornada.abiertaPor());
        assertEquals(0, jornada.nroVentas());
        assertEquals(0, jornada.totalVentas().compareTo(BigDecimal.ZERO));
    }

    @Test
    void abrirConUnaJornadaYaActivaFalla() {
        when(jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc())
                .thenReturn(Optional.of(Jornada.builder().id(1L).activa(true).build()));

        assertThrows(OperacionInvalidaException.class, () -> jornadaService.abrir("admin"));
        verify(jornadaRepository, never()).save(any());
    }

    @Test
    void elArqueoUsaUnaSolaConsultaAgregada() {
        Jornada jornada = Jornada.builder().id(1L).abiertaPor("admin").activa(true).build();
        when(jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc())
                .thenReturn(Optional.of(jornada));
        when(ventaRepository.arqueoPorJornada(EstadoVenta.ACTIVA)).thenReturn(List.<Object[]>of(
                new Object[]{1L, 4L, new BigDecimal("42000")}));

        JornadaResponse respuesta = jornadaService.activa();

        assertEquals(4, respuesta.nroVentas());
        assertEquals(0, new BigDecimal("42000").compareTo(respuesta.totalVentas()));
        verify(ventaRepository).arqueoPorJornada(EstadoVenta.ACTIVA);
        // No se carga la tabla de ventas para contar.
        verify(ventaRepository, never()).findAllByOrderByFechaDesc();
    }

    @Test
    void cerrarRegistraLaFechaDeCierre() {
        Jornada jornada = Jornada.builder().id(1L).abiertaPor("admin").activa(true).build();
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));
        when(jornadaRepository.save(any(Jornada.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ventaRepository.arqueoPorJornada(any())).thenReturn(List.of());

        JornadaResponse respuesta = jornadaService.cerrar(1L);

        assertTrue(!respuesta.activa());
        assertTrue(respuesta.fechaCierre() != null);
    }

    @Test
    void cerrarUnaJornadaYaCerradaFalla() {
        Jornada jornada = Jornada.builder().id(1L).activa(false).build();
        when(jornadaRepository.findById(1L)).thenReturn(Optional.of(jornada));

        assertThrows(OperacionInvalidaException.class, () -> jornadaService.cerrar(1L));
    }
}
