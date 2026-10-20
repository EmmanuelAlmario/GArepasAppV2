package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.model.Jornada;
import com.garepas.garepasapp.domain.repository.JornadaRepository;
import com.garepas.garepasapp.domain.repository.VentaRepository;
import com.garepas.garepasapp.web.view.JornadaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

  







@Service
@RequiredArgsConstructor
public class JornadaService {

    private final JornadaRepository jornadaRepository;
    private final VentaRepository ventaRepository;

      
    private Map<Long, Object[]> arqueoGlobal() {
        Map<Long, Object[]> arqueo = new HashMap<>();
        for (Object[] fila : ventaRepository.arqueoPorJornada(EstadoVenta.ACTIVA)) {
            arqueo.put(((Number) fila[0]).longValue(), new Object[]{fila[1], fila[2]});
        }
        return arqueo;
    }

    @Transactional(readOnly = true)
    public JornadaResponse activa() {
        return jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc()
                .map(jornada -> construirRespuesta(jornada, arqueoGlobal()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<JornadaResponse> historial() {
        Map<Long, Object[]> arqueo = arqueoGlobal();
        return jornadaRepository.findAllByOrderByFechaAperturaDesc().stream()
                .map(jornada -> construirRespuesta(jornada, arqueo))
                .toList();
    }

    private JornadaResponse construirRespuesta(Jornada jornada, Map<Long, Object[]> arqueo) {
        Object[] datos = arqueo.get(jornada.getId());
        long nroVentas = datos == null ? 0L : ((Number) datos[0]).longValue();
        BigDecimal total = datos == null ? BigDecimal.ZERO : (BigDecimal) datos[1];
        return JornadaResponse.desde(jornada, nroVentas, total);
    }

    @Transactional
    public JornadaResponse abrir(String usuario) {
        if (jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc().isPresent()) {
            throw new OperacionInvalidaException("Ya hay una jornada abierta. Cierra la jornada actual antes de abrir otra.");
        }
        Jornada jornada = Jornada.builder()
                .fechaApertura(LocalDateTime.now())
                .abiertaPor(usuario == null || usuario.isBlank() ? "usuario" : usuario)
                .activa(true)
                .build();
        return JornadaResponse.desde(jornadaRepository.save(jornada), 0L, BigDecimal.ZERO);
    }

    @Transactional
    public JornadaResponse cerrar(Long id) {
        Jornada jornada = jornadaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Jornada", id));
        if (!Boolean.TRUE.equals(jornada.getActiva())) {
            throw new OperacionInvalidaException("La jornada ya estaba cerrada");
        }
        jornada.setFechaCierre(LocalDateTime.now());
        jornada.setActiva(false);
        Jornada guardada = jornadaRepository.save(jornada);
        return construirRespuesta(guardada, arqueoGlobal());
    }
}
