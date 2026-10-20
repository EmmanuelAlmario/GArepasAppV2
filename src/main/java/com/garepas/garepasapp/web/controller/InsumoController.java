package com.garepas.garepasapp.web.controller;

import com.garepas.garepasapp.domain.command.DetalleAjusteStockCommand;
import com.garepas.garepasapp.domain.enums.UnidadMedida;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.domain.service.InsumoService;
import com.garepas.garepasapp.shared.Formato;
import com.garepas.garepasapp.web.form.AjusteStockForm;
import com.garepas.garepasapp.web.form.InsumoForm;
import com.garepas.garepasapp.web.view.InsumoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/insumos")
@RequiredArgsConstructor
public class InsumoController {

    private final InsumoService insumoService;
    private final Formato formato;

    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        String texto = q == null ? "" : q.trim().toLowerCase();
        List<InsumoResponse> insumos = insumoService.listarTodos().stream()
                .filter(i -> texto.isEmpty()
                        || (i.nombre() != null && i.nombre().toLowerCase().contains(texto))
                        || (i.categoria() != null && i.categoria().toLowerCase().contains(texto))
                        || (i.marca() != null && i.marca().toLowerCase().contains(texto))
                        || (i.id() != null && String.valueOf(i.id()).contains(texto)))
                .toList();
        model.addAttribute("insumos", insumos);
        model.addAttribute("q", q);
        return "insumos/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new InsumoForm());
        cargarUnidades(model);
        return "insumos/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        InsumoResponse insumo = insumoService.buscarPorId(id);
        InsumoForm form = new InsumoForm();
        form.setId(insumo.id());
        form.setNombre(insumo.nombre());
        form.setCategoria(insumo.categoria());
        form.setMarca(insumo.marca());
          
        form.setUnidadMedida(insumo.unidadPresentacion());
        form.setStockActual(formato.limpiar(insumo.stockPresentacion()));
        form.setStockMinimo(insumo.stockMinimoPresentacion() == null
                ? null
                : formato.limpiar(insumo.stockMinimoPresentacion()));
        form.setPrecioCompra(formato.pesos(insumo.precioPorUnidad()));
        form.setActivo(insumo.activo());
        model.addAttribute("form", form);
        cargarUnidades(model);
        return "insumos/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") InsumoForm form,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            cargarUnidades(model);
            return "insumos/form";
        }
        try {
            insumoService.crear(form.toCommand());
        } catch (RecursoDuplicadoException | OperacionInvalidaException ex) {
            model.addAttribute("error", ex.getMessage());
            cargarUnidades(model);
            return "insumos/form";
        }
        redirect.addFlashAttribute("mensaje", "Insumo creado correctamente.");
        return "redirect:/insumos";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("form") InsumoForm form,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            cargarUnidades(model);
            return "insumos/form";
        }
        try {
            insumoService.actualizar(id, form.toCommand());
        } catch (RecursoDuplicadoException | OperacionInvalidaException ex) {
            model.addAttribute("error", ex.getMessage());
            cargarUnidades(model);
            return "insumos/form";
        }
        redirect.addFlashAttribute("mensaje", "Insumo actualizado correctamente.");
        return "redirect:/insumos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        insumoService.eliminar(id);
        redirect.addFlashAttribute("mensaje", "Insumo eliminado.");
        return "redirect:/insumos";
    }

    @GetMapping("/ajustar-stock")
    public String ajustarStockForm(Model model) {
        AjusteStockForm form = new AjusteStockForm();
          
        form.getInsumoId().add(null);
        form.getCantidad().add(null);
        model.addAttribute("form", form);
        model.addAttribute("insumos", insumoService.listarActivos());
        return "insumos/ajuste";
    }

    @PostMapping("/ajustar-stock")
    public String ajustarStock(@ModelAttribute("form") AjusteStockForm form,
                               Model model, RedirectAttributes redirect,
                               Authentication authentication) {
        List<DetalleAjusteStockCommand> detalles = form.filasValidas();
        if (detalles.isEmpty()) {
            model.addAttribute("error", "Agrega al menos un insumo con cantidad distinta de cero.");
            model.addAttribute("insumos", insumoService.listarActivos());
            return "insumos/ajuste";
        }
        try {
            insumoService.ajustarStock(form.toCommand(),
                    authentication == null ? "sistema" : authentication.getName());
        } catch (OperacionInvalidaException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("insumos", insumoService.listarActivos());
            return "insumos/ajuste";
        }
        redirect.addFlashAttribute("mensaje",
                "Stock ajustado para " + detalles.size() + " insumo(s). El movimiento quedo registrado en la bitacora.");
        return "redirect:/insumos";
    }

    private void cargarUnidades(Model model) {
        model.addAttribute("unidades", UnidadMedida.values());
    }
}
