package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.AjusteStockCommand;
import com.garepas.garepasapp.domain.command.DetalleAjusteStockCommand;
import com.garepas.garepasapp.domain.command.InsumoCommand;
import com.garepas.garepasapp.domain.enums.UnidadMedida;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.model.Insumo;
import com.garepas.garepasapp.domain.repository.InsumoRepository;
import com.garepas.garepasapp.web.view.AjusteStockResponse;
import com.garepas.garepasapp.web.view.InsumoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Insumos del inventario.
 *
 * <p><b>Reglas del rediseño:</b>
 * <ul>
 *   <li>El stock y el precio se capturan en la unidad de PRESENTACION y aqui
 *       se convierten a unidad base. Antes cada forma de entrada lo hacia por
 *       su cuenta y el mismo dato significaba cosas distintas.</li>
 *   <li>Al editar, el stock se ajusta por DELTA (lo que el usuario cambio
 *       respecto al valor actual), no se sobrescribe: antes una edicion
 *       simultanea con una venta pisaba el stock y perdia unidades.</li>
 *   <li>Los ajustes dejan bitacora en {@code ajustes_inventario}.</li>
 *   <li>No se puede borrar un insumo que este en uso: antes reventaba con un
 *       error 500 por violacion de clave foranea.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class InsumoService {

    private final InsumoRepository insumoRepository;
    private final RecetaService recetaService;
    private final InventarioService inventarioService;

    @Transactional(readOnly = true)
    public List<InsumoResponse> listarTodos() {
        return insumoRepository.findAll().stream().map(InsumoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<InsumoResponse> listarActivos() {
        return insumoRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(InsumoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public InsumoResponse buscarPorId(Long id) {
        return insumoRepository.findById(id)
                .map(InsumoResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo", id));
    }

    @Transactional
    public InsumoResponse crear(InsumoCommand command) {
        if (insumoRepository.existsByNombreIgnoreCase(command.nombre())) {
            throw new RecursoDuplicadoException("Insumo", "nombre", command.nombre());
        }
        UnidadMedida unidad = command.unidadPresentacion() == null
                ? UnidadMedida.UNIDAD : command.unidadPresentacion();

        Insumo insumo = Insumo.builder()
                .nombre(command.nombre())
                .categoria(command.categoria())
                .marca(command.marca())
                .stockActual(aBase(command.stockActual(), unidad))
                .stockMinimo(aBase(command.stockMinimo(), unidad))
                .precioPorUnidadBase(precioABase(command.precioPorPresentacion(), unidad))
                .unidadPresentacion(unidad)
                .activo(Boolean.TRUE.equals(command.activo()))
                .build();
        return InsumoResponse.desde(insumoRepository.save(insumo));
    }

    @Transactional
    public InsumoResponse actualizar(Long id, InsumoCommand command) {
        Insumo insumo = insumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo", id));
        if (!insumo.getNombre().equalsIgnoreCase(command.nombre())
                && insumoRepository.existsByNombreIgnoreCase(command.nombre())) {
            throw new RecursoDuplicadoException("Insumo", "nombre", command.nombre());
        }

        UnidadMedida unidad = command.unidadPresentacion() == null
                ? insumo.getUnidadPresentacion() : command.unidadPresentacion();

        // El stock llega como valor absoluto en unidad de presentacion: se
        // convierte el delta para no pisar ventas concurrentes.
        BigDecimal stockSolicitado = aBase(command.stockActual(), unidad);
        BigDecimal delta = stockSolicitado.subtract(insumo.getStockActual());

        insumo.setNombre(command.nombre());
        insumo.setCategoria(command.categoria());
        insumo.setMarca(command.marca());
        insumo.setStockMinimo(aBase(command.stockMinimo(), unidad));
        insumo.setPrecioPorUnidadBase(precioABase(command.precioPorPresentacion(), unidad));
        insumo.setUnidadPresentacion(unidad);
        insumo.setActivo(Boolean.TRUE.equals(command.activo()));

        if (delta.compareTo(BigDecimal.ZERO) != 0) {
            // El delta esta en unidad base: ajustarInsumo lo aplica y deja bitacora.
            insumoRepository.save(insumo);
            inventarioService.ajustarInsumo(insumo, delta, "Edicion de ficha de insumo", "sistema");
        } else {
            insumoRepository.save(insumo);
        }

        // Si cambio el precio de un insumo, los insumos fabricados derivados de
        // sus recetas cambian de precio tambien.
        recetaService.recalcularInsumosProducidos(id);

        return InsumoResponse.desde(insumo);
    }

    @Transactional
    public void eliminar(Long id) {
        Insumo insumo = insumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo", id));

        if (!insumoRepository.findRecetasQueUsan(id).isEmpty()) {
            throw new OperacionInvalidaException(
                    "El insumo '" + insumo.getNombre() + "' esta en uso en una o mas recetas");
        }
        if (insumoRepository.countProduccionesQueUsan(id) > 0) {
            throw new OperacionInvalidaException(
                    "El insumo '" + insumo.getNombre() + "' tiene producciones registradas");
        }
        if (insumoRepository.countRecetasQueProducen(id) > 0) {
            throw new OperacionInvalidaException(
                    "El insumo '" + insumo.getNombre() + "' es fabricado por una receta");
        }
        insumoRepository.deleteById(id);
    }

    /**
     * Ajusta el stock de varios insumos en una sola transaccion atomica.
     *
     * <p>La cantidad es un delta en la unidad de PRESENTACION del insumo:
     * positiva repone (compra), negativa consume (merma). Si algun insumo
     * quedaria en negativo, no se aplica NINGUNO de los ajustes.
     *
     * <p>Un ajuste de compra no genera gasto: el gasto de materia prima se
     * reconoce cuando el insumo se consume en una produccion.
     */
    @Transactional
    public AjusteStockResponse ajustarStock(AjusteStockCommand command, String usuario) {
        if (command.detalles() == null || command.detalles().isEmpty()) {
            throw new OperacionInvalidaException("Debe incluir al menos un insumo para ajustar");
        }

        // Consolidar deltas por insumo (el formulario puede repetir un insumo).
        Map<Long, BigDecimal> deltasPresentacion = new LinkedHashMap<>();
        for (DetalleAjusteStockCommand detalle : command.detalles()) {
            if (detalle.insumoId() == null) {
                throw new OperacionInvalidaException("El insumo es obligatorio");
            }
            if (detalle.cantidad() == null || detalle.cantidad().compareTo(BigDecimal.ZERO) == 0) {
                throw new OperacionInvalidaException("La cantidad de ajuste no puede ser cero.");
            }
            deltasPresentacion.merge(detalle.insumoId(), detalle.cantidad(), BigDecimal::add);
        }

        // Bloquear los insumos (siempre ordenados por id).
        Map<Long, Insumo> insumos = inventarioService.bloquearInsumos(deltasPresentacion.keySet());

        // Convertir a unidad base y validar antes de tocar nada.
        Map<Long, BigDecimal> deltasBase = new LinkedHashMap<>();
        List<String> errores = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : deltasPresentacion.entrySet()) {
            Insumo insumo = insumos.get(entry.getKey());
            BigDecimal deltaBase = insumo.getUnidadPresentacion().aBase(entry.getValue());
            deltasBase.put(entry.getKey(), deltaBase);
            BigDecimal nuevoStock = insumo.getStockActual().add(deltaBase);
            if (nuevoStock.compareTo(BigDecimal.ZERO) < 0) {
                errores.add("'" + insumo.getNombre() + "': stock actual " + insumo.stockEnPresentacion()
                        + ", ajuste solicitado " + entry.getValue());
            }
        }
        if (!errores.isEmpty()) {
            throw new OperacionInvalidaException(
                    "El ajuste dejaria el stock negativo en uno o mas insumos. " + String.join(" | ", errores));
        }

        BigDecimal valorTotal = BigDecimal.ZERO;
        List<InsumoResponse> actualizados = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : deltasBase.entrySet()) {
            Insumo insumo = insumos.get(entry.getKey());
            inventarioService.ajustarInsumo(insumo, entry.getValue(), "Ajuste manual de inventario", usuario);
            valorTotal = valorTotal.add(entry.getValue().abs().multiply(insumo.getPrecioPorUnidadBase()));
            actualizados.add(InsumoResponse.desde(insumo));
        }

        return new AjusteStockResponse(actualizados.size(),
                valorTotal.setScale(2, RoundingMode.HALF_UP), actualizados);
    }

    private BigDecimal aBase(BigDecimal cantidad, UnidadMedida unidad) {
        if (cantidad == null) {
            return BigDecimal.ZERO;
        }
        return unidad.aBase(cantidad);
    }

    private BigDecimal precioABase(BigDecimal precio, UnidadMedida unidad) {
        if (precio == null) {
            return BigDecimal.ZERO;
        }
        return unidad.precioABase(precio);
    }
}
