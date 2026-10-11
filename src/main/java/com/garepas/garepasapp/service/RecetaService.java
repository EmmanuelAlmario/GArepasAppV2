package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.request.DetalleRecetaRequest;
import com.garepas.garepasapp.dto.request.RecetaRequest;
import com.garepas.garepasapp.dto.response.RecetaResponse;
import com.garepas.garepasapp.entity.DetalleReceta;
import com.garepas.garepasapp.entity.Insumo;
import com.garepas.garepasapp.entity.Receta;
import com.garepas.garepasapp.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.repository.InsumoRepository;
import com.garepas.garepasapp.repository.ProductoRepository;
import com.garepas.garepasapp.repository.RecetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RecetaService {

    private final RecetaRepository recetaRepository;
    private final InsumoRepository insumoRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<RecetaResponse> listarTodas() {
        return recetaRepository.findAll()
                .stream()
                .map(RecetaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecetaResponse buscarPorId(Long id) {
        return recetaRepository.findById(id)
                .map(RecetaResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta", id));
    }

    @Transactional
    public RecetaResponse crear(RecetaRequest request) {
        if (recetaRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new RecursoDuplicadoException("Receta", "nombre", request.nombre());
        }
        Receta receta = Receta.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .build();

        Map<Long, Insumo> insumos = cargarInsumos(request.detalles());
        List<DetalleReceta> detalles = request.detalles().stream()
                .map(dr -> DetalleReceta.builder()
                        .receta(receta)
                        .insumo(insumos.get(dr.insumoId()))
                        .cantidad(dr.cantidad())
                        .unidadMedida(dr.unidadMedida())
                        .build())
                .toList();

        receta.getDetalles().addAll(detalles);
        aplicarInsumoProducido(receta, request, insumos);
        Receta guardada = recetaRepository.save(receta);
        recalcularPrecioProducido(guardada);
        return RecetaResponse.desde(guardada);
    }

    @Transactional
    public RecetaResponse actualizar(Long id, RecetaRequest request) {
        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta", id));

        if (!receta.getNombre().equalsIgnoreCase(request.nombre()) &&
                recetaRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new RecursoDuplicadoException("Receta", "nombre", request.nombre());
        }

        receta.setNombre(request.nombre());
        receta.setDescripcion(request.descripcion());

        Map<Long, Insumo> insumos = cargarInsumos(request.detalles());
        aplicarInsumoProducido(receta, request, insumos);

        // Reemplazo total pero conservando la lista existente para respetar orphanRemoval
        receta.getDetalles().clear();
        for (DetalleRecetaRequest dr : request.detalles()) {
            receta.getDetalles().add(DetalleReceta.builder()
                    .receta(receta)
                    .insumo(insumos.get(dr.insumoId()))
                    .cantidad(dr.cantidad())
                    .unidadMedida(dr.unidadMedida())
                    .build());
        }

        Receta guardada = recetaRepository.save(receta);
        recalcularPrecioProducido(guardada);
        return RecetaResponse.desde(guardada);
    }

    /**
     * Recalcula el precio por gramo de todos los insumos fabricados (costo de su
     * receta / rendimiento). Se invoca cuando cambia el precio de un ingrediente.
     */
    @Transactional
    public void recalcularInsumosProducidos() {
        for (Receta receta : recetaRepository.findByInsumoProducidoIsNotNull()) {
            recalcularPrecioProducido(receta);
        }
    }

    /** Precio por gramo automatico = Σ(cantidad × precio ingrediente) / rendimiento. */
    private void recalcularPrecioProducido(Receta receta) {
        Insumo producido = receta.getInsumoProducido();
        if (producido == null) {
            return;
        }
        BigDecimal costo = CostoService.costoDeReceta(receta);
        producido.setPrecioPorGramo(costo.divide(receta.getRendimiento(), 6, RoundingMode.HALF_UP));
        insumoRepository.save(producido);
    }

    /** Valida y asigna el insumo fabricado (o lo limpia si la receta es de producto). */
    private void aplicarInsumoProducido(Receta receta, RecetaRequest request, Map<Long, Insumo> insumos) {
        if (request.insumoProducidoId() == null) {
            receta.setInsumoProducido(null);
            receta.setRendimiento(null);
            return;
        }
        if (request.rendimiento() == null || request.rendimiento().signum() <= 0) {
            throw new OperacionInvalidaException(
                    "El rendimiento debe ser mayor a cero para una receta que fabrica un insumo.");
        }
        Insumo producido = insumoRepository.findById(request.insumoProducidoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo", request.insumoProducidoId()));

        for (DetalleRecetaRequest dr : request.detalles()) {
            if (producido.getId().equals(dr.insumoId())) {
                throw new OperacionInvalidaException(
                        "El insumo fabricado no puede ser ingrediente de su propia receta.");
            }
            if (!recetaRepository.findByInsumoProducidoId(dr.insumoId()).isEmpty()) {
                throw new OperacionInvalidaException("El ingrediente '" + insumos.get(dr.insumoId()).getNombre()
                        + "' es un insumo fabricado; no puede usarse en una receta que tambien fabrica insumos.");
            }
        }

        boolean fabricadoPorOtra = recetaRepository.findByInsumoProducidoId(producido.getId()).stream()
                .anyMatch(r -> !Objects.equals(r.getId(), receta.getId()));
        if (fabricadoPorOtra) {
            throw new OperacionInvalidaException(
                    "El insumo '" + producido.getNombre() + "' ya es fabricado por otra receta.");
        }

        receta.setInsumoProducido(producido);
        receta.setRendimiento(request.rendimiento());
    }

    @Transactional
    public void eliminar(Long id) {
        if (!recetaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Receta", id);
        }
        if (!productoRepository.findByRecetaId(id).isEmpty()) {
            throw new OperacionInvalidaException(
                    "No se puede eliminar la receta porque está asociada a uno o más productos.");
        }
        recetaRepository.deleteById(id);
    }

    private Map<Long, Insumo> cargarInsumos(List<DetalleRecetaRequest> detalles) {
        List<Long> ids = detalles.stream().map(DetalleRecetaRequest::insumoId).distinct().toList();
        Map<Long, Insumo> map = new HashMap<>();
        for (Insumo i : insumoRepository.findAllById(ids)) {
            map.put(i.getId(), i);
        }
        for (Long id : ids) {
            if (!map.containsKey(id)) {
                throw new RecursoNoEncontradoException("Insumo", id);
            }
        }
        return map;
    }
}
