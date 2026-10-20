package com.garepas.garepasapp.web.controller;

import com.garepas.garepasapp.web.form.RecetaForm;
import com.garepas.garepasapp.web.view.DetalleRecetaResponse;
import com.garepas.garepasapp.web.view.RecetaResponse;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.enums.UnidadMedida;
import com.garepas.garepasapp.domain.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.domain.service.InsumoService;
import com.garepas.garepasapp.domain.service.RecetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recetas")
@RequiredArgsConstructor
public class RecetaController {

    private final RecetaService recetaService;
    private final InsumoService insumoService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("recetas", recetaService.listarTodas());
        return "recetas/list";
    }

    @GetMapping("/nuevo")
    public String nueva(Model model) {
        RecetaForm form = new RecetaForm();
        // Una fila vacia inicial para que el formulario muestre los selectores.
        form.getInsumoId().add(null);
        form.getCantidad().add(null);
        form.getUnidadMedida().add(null);
        model.addAttribute("form", form);
        cargarListas(model);
        return "recetas/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        RecetaResponse receta = recetaService.buscarPorId(id);
        RecetaForm form = new RecetaForm();
        form.setId(receta.id());
        form.setNombre(receta.nombre());
        form.setDescripcion(receta.descripcion());
        form.setInsumoProducidoId(receta.insumoProducidoId());
        form.setRendimiento(receta.rendimiento());
        for (DetalleRecetaResponse detalle : receta.detalles()) {
            form.getInsumoId().add(detalle.insumoId());
            // La cantidad se muestra en la unidad de presentacion del insumo
            // (lo que el usuario maneja); el servicio la lleva a unidad base.
            form.getCantidad().add(detalle.cantidadEnPresentacion());
            form.getUnidadMedida().add(detalle.insumoUnidadPresentacion());
        }
        model.addAttribute("form", form);
        cargarListas(model);
        return "recetas/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") RecetaForm form,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            cargarListas(model);
            return "recetas/form";
        }
        if (form.filasValidas().isEmpty()) {
            model.addAttribute("error", "Agrega al menos un insumo con cantidad mayor a cero.");
            cargarListas(model);
            return "recetas/form";
        }
        try {
            recetaService.crear(form.toCommand());
        } catch (RecursoDuplicadoException | OperacionInvalidaException ex) {
            model.addAttribute("error", ex.getMessage());
            cargarListas(model);
            return "recetas/form";
        }
        redirect.addFlashAttribute("mensaje", "Receta creada correctamente.");
        return "redirect:/recetas";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("form") RecetaForm form,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            cargarListas(model);
            return "recetas/form";
        }
        if (form.filasValidas().isEmpty()) {
            model.addAttribute("error", "Agrega al menos un insumo con cantidad mayor a cero.");
            cargarListas(model);
            return "recetas/form";
        }
        try {
            recetaService.actualizar(id, form.toCommand());
        } catch (RecursoDuplicadoException | OperacionInvalidaException ex) {
            model.addAttribute("error", ex.getMessage());
            cargarListas(model);
            return "recetas/form";
        }
        redirect.addFlashAttribute("mensaje", "Receta actualizada correctamente.");
        return "redirect:/recetas";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            recetaService.eliminar(id);
            redirect.addFlashAttribute("mensaje", "Receta eliminada.");
        } catch (OperacionInvalidaException ex) {
            redirect.addFlashAttribute("mensaje", ex.getMessage());
            redirect.addFlashAttribute("tipo", "danger");
        }
        return "redirect:/recetas";
    }

    private void cargarListas(Model model) {
        model.addAttribute("insumos", insumoService.listarActivos());
        model.addAttribute("unidades", UnidadMedida.values());
    }
}