package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.response.JornadaResponse;
import com.garepas.garepasapp.entity.Jornada;
import com.garepas.garepasapp.entity.Venta;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.repository.JornadaRepository;
import com.garepas.garepasapp.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JornadaService {

    private final JornadaRepository jornadaRepository;
    private final VentaRepository ventaRepository;

    private JornadaResponse arqueo(Jornada jornada) {
        List<Venta> ventas = ventaRepository.findAllByOrderByFechaDesc()
                .stream()
                .filter(v -> v.getJornadaId() != null && v.getJornadaId().equals(jornada.getId()))
                .toList();
        int nro = ventas.size();
        BigDecimal total = ventas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return JornadaResponse.desde(jornada, nro, total);
    }

    @Transactional(readOnly = true)
    public JornadaResponse activa() {
        return jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc()
                .map(this::arqueo)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<JornadaResponse> historial() {
        return jornadaRepository.findAllByOrderByFechaAperturaDesc()
                .stream()
                .map(this::arqueo)
                .toList();
    }

    @Transactional
    public JornadaResponse abrir(String usuario) {
        if (jornadaRepository.findFirstByActivaTrueOrderByFechaAperturaDesc().isPresent()) {
            throw new OperacionInvalidaException("Ya hay una jornada abierta. Ciérrala antes de abrir otra.");
        }
        String actor = (usuario == null || usuario.isBlank()) ? "usuario" : usuario;
        Jornada jornada = Jornada.builder()
                .fechaApertura(LocalDateTime.now())
                .abiertaPor(actor)
                .activa(Boolean.TRUE)
                .build();
        return arqueo(jornadaRepository.save(jornada));
    }

    @Transactional
    public JornadaResponse cerrar(Long id) {
        Jornada jornada = jornadaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Jornada", id));
        if (!Boolean.TRUE.equals(jornada.getActiva())) {
            throw new OperacionInvalidaException("La jornada ya está cerrada");
        }
        jornada.setActiva(Boolean.FALSE);
        jornada.setFechaCierre(LocalDateTime.now());
        return arqueo(jornadaRepository.save(jornada));
    }
}