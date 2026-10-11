package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.response.JornadaResponse;
import com.garepas.garepasapp.entity.Jornada;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.repository.JornadaRepository;
import com.garepas.garepasapp.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Ciclo de la jornada de operación: solo una abierta a la vez y cierre
 * con fecha registrada.
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
        when(ventaRepository.findAllByOrderByFechaDesc()).thenReturn(List.of());
        when(jornadaRepository.save(any(Jornada.class))).thenAnswer(inv -> {
            Jornada jornada = inv.getArgument(0);
            if (jornada.getId() == null) {
                jornada.setId(1L);
            }
            return jornada;
        });
    }

    @Test
    void abrirCreaJornadaActivaConElUsuarioQueLaAbre() {
        when(jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc()).thenReturn(Optional.empty());

        JornadaResponse respuesta = jornadaService.abrir("admin");

        assertTrue(respuesta.activa());
        assertEquals("admin", respuesta.abiertaPor());
        assertNotNull(respuesta.fechaApertura());
        assertEquals(0, respuesta.nroVentas());
        verify(jornadaRepository).save(any(Jornada.class));
    }

    @Test
    void abrirConJornadaYaActivaFalla() {
        when(jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc())
                .thenReturn(Optional.of(Jornada.builder().id(3L).activa(true).build()));

        assertThrows(OperacionInvalidaException.class, () -> jornadaService.abrir("admin"));

        verify(jornadaRepository, never()).save(any());
    }

    @Test
    void cerrarDesactivaLaJornadaYRegistraFechaDeCierre() {
        Jornada jornada = Jornada.builder()
                .id(2L)
                .fechaApertura(LocalDateTime.now().minusHours(8))
                .abiertaPor("admin")
                .activa(true)
                .build();
        when(jornadaRepository.findById(2L)).thenReturn(Optional.of(jornada));

        JornadaResponse respuesta = jornadaService.cerrar(2L);

        assertFalse(respuesta.activa());
        assertNotNull(respuesta.fechaCierre());
        verify(jornadaRepository).save(jornada);
    }

    @Test
    void cerrarUnaJornadaYaCerradaFalla() {
        Jornada jornada = Jornada.builder().id(2L).activa(false).build();
        when(jornadaRepository.findById(2L)).thenReturn(Optional.of(jornada));

        assertThrows(OperacionInvalidaException.class, () -> jornadaService.cerrar(2L));

        verify(jornadaRepository, never()).save(any());
    }
}