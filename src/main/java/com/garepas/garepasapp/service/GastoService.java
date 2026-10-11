package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.request.GastoRequest;
import com.garepas.garepasapp.dto.response.GastoResponse;
import com.garepas.garepasapp.entity.Gasto;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.repository.GastoRepository;
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
        return gastoRepository.findAllByOrderByFechaDesc()
                .stream()
                .map(GastoResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<GastoResponse> listarPaginado(Pageable pageable) {
        return gastoRepository.findAllByOrderByFechaDesc(pageable).map(GastoResponse::desde);
    }

    @Transactional(readOnly = true)
    public GastoResponse buscarPorId(Long id) {
        return gastoRepository.findById(id)
                .map(GastoResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gasto", id));
    }

    @Transactional
    public GastoResponse registrar(GastoRequest request) {
        Gasto gasto = Gasto.builder()
                .descripcion(request.descripcion())
                .monto(request.monto())
                .categoria(request.categoria())
                .fecha(LocalDateTime.now())
                .build();
        return GastoResponse.desde(gastoRepository.save(gasto));
    }

    @Transactional
    public GastoResponse actualizar(Long id, GastoRequest request) {
        Gasto gasto = gastoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gasto", id));
        if (gasto.getProduccionId() != null) {
            throw new OperacionInvalidaException(
                    "Este gasto fue generado automáticamente por una producción y no se puede editar.");
        }
        gasto.setDescripcion(request.descripcion());
        gasto.setMonto(request.monto());
        gasto.setCategoria(request.categoria());
        return GastoResponse.desde(gastoRepository.save(gasto));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!gastoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Gasto", id);
        }
        gastoRepository.deleteById(id);
    }
}
