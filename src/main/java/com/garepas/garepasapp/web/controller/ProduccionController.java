package com.garepas.garepasapp.web.controller;

import com.garepas.garepasapp.domain.enums.EstadoProduccion;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.service.ProduccionService;
import com.garepas.garepasapp.domain.service.ProductoService;
import com.garepas.garepasapp.web.form.ProduccionForm;
import com.garepas.garepasapp.web.view.ProduccionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Producciones: registrar (UC16), re-verificar pendientes (UC17),
 * ver insumos faltantes (UC18) y eliminar (UC19).
 */
@Controller
@RequestMapping("/producciones")
@RequiredArgsConstructor
public class ProduccionController {

    private final ProduccionService produccionService;
    private final ProductoService productoService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("producciones", produccionService.listarTodas());
        return "producciones/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new ProduccionForm());
        cargarProductos(model);
        return "producciones/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") ProduccionForm form,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            cargarProductos(model);
            return "producciones/form";
        }
        try {
            ProduccionResponse produccion = produccionService.registrar(form.toCommand());
            if (produccion.estado() == EstadoProduccion.PENDIENTE) {
                redirect.addFlashAttribute("mensaje",
                        "Produccion registrada como INSUFICIENTE: faltan insumos. Reverificala cuando repongas stock.");
                redirect.addFlashAttribute("tipo", "warning");
            } else {
                redirect.addFlashAttribute("mensaje",
                        "Produccion completada: stock descontado y gasto de materia prima registrado.");
            }
        } catch (RecursoNoEncontradoException | OperacionInvalidaException ex) {
            model.addAttribute("error", ex.getMessage());
            cargarProductos(model);
            return "producciones/form";
        }
        return "redirect:/producciones";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        ProduccionResponse produccion = produccionService.buscarPorId(id);
        long insumosOk = produccion.detalles().stream()
                .filter(d -> Boolean.TRUE.equals(d.suficiente()))
                .count();
        // Compara el stock actual con lo requerido: si ya alcanza, se invita a re-verificar
        // sin presentar como vigente el resultado de una verificacion anterior.
        boolean tieneStockAhora = !produccion.detalles().isEmpty() && produccion.detalles().stream()
                .allMatch(d -> d.stockDisponible() != null && d.cantidadRequerida() != null
                        && d.stockDisponible().compareTo(d.cantidadRequerida()) >= 0);
        model.addAttribute("p", produccion);
        model.addAttribute("insumosOk", insumosOk);
        model.addAttribute("tieneStockAhora", tieneStockAhora);
        return "producciones/detalle";
    }

    @PostMapping("/{id}/verificar")
    public String verificar(@PathVariable Long id, RedirectAttributes redirect) {
        ProduccionResponse produccion = produccionService.verificarYCompletar(id);
        if (produccion.estado() == EstadoProduccion.COMPLETADA) {
            redirect.addFlashAttribute("mensaje",
                    "Produccion completada: stock descontado y gasto de materia prima registrado.");
        } else {
            redirect.addFlashAttribute("mensaje", "Aun faltan insumos para completar la produccion.");
            redirect.addFlashAttribute("tipo", "warning");
        }
        return "redirect:/producciones";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        produccionService.eliminar(id);
        redirect.addFlashAttribute("mensaje", "Produccion eliminada y stock revertido.");
        return "redirect:/producciones";
    }

    /** Solo los productos con receta asignada pueden producirse. */
    private void cargarProductos(Model model) {
        model.addAttribute("productos", productoService.listarActivos().stream()
                .filter(p -> p.recetaId() != null)
                .toList());
    }
}
