package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.GastoCommand;
import com.garepas.garepasapp.domain.enums.CategoriaGasto;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.model.Gasto;
import com.garepas.garepasapp.domain.repository.GastoRepository;
import com.garepas.garepasapp.web.view.GastoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

  












@Service
@RequiredArgsConstructor
public class GastoService {

    private final GastoRepository gastoRepository;

    @Transactional(readOnly = true)
    public List<GastoResponse> listarTodos() {
        return gastoRepository.findAllByOrderByFechaDesc().stream().map(GastoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public Page<GastoResponse> listarPaginado(Pageable pageable) {
        return gastoRepository.findAllByOrderByFechaDesc(pageable).map(GastoResponse::desde);
    }

      
    @Transactional(readOnly = true)
    public Page<GastoResponse> buscar(String texto, CategoriaGasto categoria, Pageable pageable) {
        String q = texto == null ? "" : texto.trim();
        return gastoRepository.buscar(q, categoria, pageable).map(GastoResponse::desde);
    }

    @Transactional(readOnly = true)
    public GastoResponse buscarPorId(Long id) {
        return gastoRepository.findById(id)
                .map(GastoResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gasto", id));
    }

    @Transactional
    public GastoResponse registrar(GastoCommand command) {
        if (command.monto() == null || command.monto().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new OperacionInvalidaException("El monto del gasto debe ser mayor que cero");
        }
        Gasto gasto = Gasto.builder()
                .descripcion(command.descripcion())
                .monto(command.monto())
                .categoria(command.categoria())
                .fecha(command.fecha() == null ? LocalDateTime.now() : command.fecha())
                .build();
        return GastoResponse.desde(gastoRepository.save(gasto));
    }

    @Transactional
    public GastoResponse actualizar(Long id, GastoCommand command) {
        Gasto gasto = gastoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gasto", id));
        if (gasto.esAutomatico()) {
            throw new OperacionInvalidaException(
                    "Este gasto fue generado automaticamente por una produccion y no se puede editar");
        }
        if (command.monto() == null || command.monto().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new OperacionInvalidaException("El monto del gasto debe ser mayor que cero");
        }
        gasto.setDescripcion(command.descripcion());
        gasto.setMonto(command.monto());
        gasto.setCategoria(command.categoria());
        if (command.fecha() != null) {
            gasto.setFecha(command.fecha());
        }
        return GastoResponse.desde(gastoRepository.save(gasto));
    }

    @Transactional
    public void eliminar(Long id) {
        Gasto gasto = gastoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gasto", id));
        if (gasto.esAutomatico()) {
            throw new OperacionInvalidaException(
                    "Este gasto fue generado automaticamente por una produccion y no se puede eliminar");
        }
        gastoRepository.deleteById(id);
    }
}
