package com.garepas.garepasapp.controller;

import com.garepas.garepasapp.dto.form.GastoForm;
import com.garepas.garepasapp.dto.response.GastoResponse;
import com.garepas.garepasapp.enums.CategoriaGasto;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.service.GastoService;
import com.garepas.garepasapp.util.Formato;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gastos operativos (UC25-UC28). Los gastos generados automáticamente por una
 * produccion (categoria MATERIA_PRIMA) no se editan ni eliminan desde aqui.
 */
@Controller
@RequestMapping("/gastos")
@RequiredArgsConstructor
public class GastoController {

    private final GastoService gastoService;
    private final Formato formato;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("gastos", gastoService.listarTodos());
        return "gastos/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new GastoForm());
        cargarCategorias(model);
        return "gastos/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        GastoResponse gasto = gastoService.buscarPorId(id);
        if (gasto.produccionId() != null) {
            redirect.addFlashAttribute("mensaje",
                    "Este gasto fue generado automáticamente por una producción y no se puede editar.");
            redirect.addFlashAttribute("tipo", "warning");
            return "redirect:/gastos";
        }
        GastoForm form = new GastoForm();
        form.setId(gasto.id());
        form.setDescripcion(gasto.descripcion());
        form.setMonto(formato.pesos(gasto.monto()));
        form.setCategoria(gasto.categoria());
        model.addAttribute("form", form);
        cargarCategorias(model);
        return "gastos/form";
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute("form") GastoForm form,
                            BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            cargarCategorias(model);
            return "gastos/form";
        }
        gastoService.registrar(form.toRequest());
        redirect.addFlashAttribute("mensaje", "Gasto registrado correctamente.");
        return "redirect:/gastos";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("form") GastoForm form,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            cargarCategorias(model);
            return "gastos/form";
        }
        try {
            gastoService.actualizar(id, form.toRequest());
        } catch (OperacionInvalidaException ex) {
            redirect.addFlashAttribute("mensaje", ex.getMessage());
            redirect.addFlashAttribute("tipo", "warning");
            return "redirect:/gastos";
        }
        redirect.addFlashAttribute("mensaje", "Gasto actualizado correctamente.");
        return "redirect:/gastos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        GastoResponse gasto = gastoService.buscarPorId(id);
        if (gasto.produccionId() != null) {
            redirect.addFlashAttribute("mensaje",
                    "No se puede eliminar: es el gasto de materia prima de una producción. Elimina la producción para revertirlo.");
            redirect.addFlashAttribute("tipo", "warning");
        } else {
            gastoService.eliminar(id);
            redirect.addFlashAttribute("mensaje", "Gasto eliminado.");
        }
        return "redirect:/gastos";
    }

    private void cargarCategorias(Model model) {
        model.addAttribute("categorias", CategoriaGasto.values());
    }
}