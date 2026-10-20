package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.DetalleRecetaCommand;
import com.garepas.garepasapp.domain.command.RecetaCommand;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.model.DetalleReceta;
import com.garepas.garepasapp.domain.model.Insumo;
import com.garepas.garepasapp.domain.model.Producto;
import com.garepas.garepasapp.domain.model.Receta;
import com.garepas.garepasapp.domain.repository.InsumoRepository;
import com.garepas.garepasapp.domain.repository.ProductoRepository;
import com.garepas.garepasapp.domain.repository.RecetaRepository;
import com.garepas.garepasapp.web.view.RecetaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

  


















@Service
@RequiredArgsConstructor
public class RecetaService {

    private final RecetaRepository recetaRepository;
    private final InsumoRepository insumoRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<RecetaResponse> listarTodas() {
        return recetaRepository.findAll().stream().map(RecetaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public RecetaResponse buscarPorId(Long id) {
        return recetaRepository.findById(id)
                .map(RecetaResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta", id));
    }

    @Transactional
    public RecetaResponse crear(RecetaCommand command) {
        if (recetaRepository.existsByNombreIgnoreCase(command.nombre())) {
            throw new RecursoDuplicadoException("Receta", "nombre", command.nombre());
        }
        Receta receta = Receta.builder()
                .nombre(command.nombre())
                .descripcion(command.descripcion())
                .build();
        aplicar(receta, command);
        return RecetaResponse.desde(recetaRepository.save(receta));
    }

    @Transactional
    public RecetaResponse actualizar(Long id, RecetaCommand command) {
        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta", id));
        if (!receta.getNombre().equalsIgnoreCase(command.nombre())
                && recetaRepository.existsByNombreIgnoreCase(command.nombre())) {
            throw new RecursoDuplicadoException("Receta", "nombre", command.nombre());
        }
        receta.setNombre(command.nombre());
        receta.setDescripcion(command.descripcion());
        aplicar(receta, command);
        return RecetaResponse.desde(recetaRepository.save(receta));
    }

      



    private void aplicar(Receta receta, RecetaCommand command) {
        List<DetalleRecetaCommand> comandos = command.detalles() == null ? List.of() : command.detalles();

          
        Map<Long, BigDecimal> cantidades = new LinkedHashMap<>();
        Map<Long, UnidadConUnidad> unidades = new LinkedHashMap<>();
        for (DetalleRecetaCommand detalle : comandos) {
            if (detalle.insumoId() == null || detalle.cantidad() == null || detalle.unidad() == null) {
                continue;
            }
            if (detalle.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new OperacionInvalidaException("Las cantidades de la receta deben ser mayores que cero");
            }
            BigDecimal enBase = detalle.unidad().aBase(detalle.cantidad());
            cantidades.merge(detalle.insumoId(), enBase, BigDecimal::add);
            unidades.put(detalle.insumoId(), new UnidadConUnidad(detalle.cantidad(), detalle.unidad()));
        }
        if (cantidades.isEmpty()) {
            throw new OperacionInvalidaException("La receta debe incluir al menos un ingrediente");
        }

          
        Insumo insumoProducido = null;
        if (command.insumoProducidoId() != null) {
            insumoProducido = insumoRepository.findById(command.insumoProducidoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Insumo", command.insumoProducidoId()));
            if (command.rendimiento() == null || command.rendimiento().compareTo(BigDecimal.ZERO) <= 0) {
                throw new OperacionInvalidaException(
                        "Una receta que fabrica un insumo necesita indicar su rendimiento (cuanto rinde un lote)");
            }
        }

        List<DetalleReceta> detalles = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : cantidades.entrySet()) {
            Insumo insumo = insumoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Insumo", entry.getKey()));
            UnidadConUnidad unidad = unidades.get(entry.getKey());

            if (!unidad.unidad().compatibleCon(insumo.unidadBase())) {
                throw new OperacionInvalidaException(
                        "La unidad " + unidad.unidad().getEtiqueta() + " no es compatible con el insumo '"
                                + insumo.getNombre() + "' (se maneja en " + insumo.unidadBase().getEtiqueta() + ")");
            }

              
              
            if (insumoProducido != null && insumo.getId().equals(insumoProducido.getId())) {
                throw new OperacionInvalidaException(
                        "Un insumo no puede ser ingrediente de la receta que lo fabrica");
            }
            if (insumoProducido != null && esInsumoFabricado(insumo.getId())) {
                throw new OperacionInvalidaException(
                        "El insumo '" + insumo.getNombre()
                                + "' ya es fabricado por otra receta y no puede usarse en una receta que tambien fabrica insumos");
            }

            detalles.add(DetalleReceta.builder()
                    .receta(receta)
                    .insumo(insumo)
                    .cantidad(entry.getValue())
                    .build());
        }

        receta.getDetalles().clear();
        receta.getDetalles().addAll(detalles);
        receta.setInsumoProducido(insumoProducido);
        receta.setRendimiento(insumoProducido != null
                ? command.rendimiento()
                : (command.rendimiento() == null ? BigDecimal.ONE : command.rendimiento()));

        recalcularPrecioProducido(receta);
    }

      
    private boolean esInsumoFabricado(Long insumoId) {
        return !recetaRepository.findByInsumoProducidoId(insumoId).isEmpty();
    }

      



    private void recalcularPrecioProducido(Receta receta) {
        if (receta.getInsumoProducido() == null) {
            return;
        }
        BigDecimal costo = CostoService.costoDeReceta(receta);
        BigDecimal rendimiento = CostoService.rendimientoDe(receta);
        BigDecimal precio = costo.divide(rendimiento, 6, RoundingMode.HALF_UP);
        Insumo producido = receta.getInsumoProducido();
        if (producido.getPrecioPorUnidadBase() == null
                || producido.getPrecioPorUnidadBase().compareTo(precio) != 0) {
            producido.setPrecioPorUnidadBase(precio);
            insumoRepository.save(producido);
        }
    }

      







    @Transactional
    public void recalcularInsumosProducidos(Long insumoAfectado) {
        if (insumoAfectado == null) {
            for (Receta receta : recetaRepository.findByInsumoProducidoIsNotNull()) {
                recalcularPrecioProducido(receta);
            }
            return;
        }
          
        List<Receta> recetas = new ArrayList<>(insumoRepository.findRecetasQueUsan(insumoAfectado));
          
          
        recetas.addAll(recetaRepository.findByInsumoProducidoId(insumoAfectado));
        for (Receta receta : recetas) {
            if (receta.getInsumoProducido() != null) {
                recalcularPrecioProducido(receta);
            }
        }
    }

    @Transactional
    public void eliminar(Long id) {
        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta", id));
        if (!productoRepository.findByRecetaId(id).isEmpty()) {
            throw new OperacionInvalidaException(
                    "La receta esta asignada a uno o mas productos y no se puede eliminar");
        }
        if (!recetaRepository.findByInsumoProducidoId(id).isEmpty()) {
            throw new OperacionInvalidaException(
                    "La receta es el origen de un insumo fabricado y no se puede eliminar");
        }
        recetaRepository.deleteById(id);
    }

      
    private record UnidadConUnidad(BigDecimal cantidad, com.garepas.garepasapp.domain.enums.UnidadMedida unidad) {
    }
}
