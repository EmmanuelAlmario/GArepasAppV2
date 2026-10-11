package com.garepas.garepasapp.controller;

import com.garepas.garepasapp.dto.form.AjusteStockForm;
import com.garepas.garepasapp.dto.form.InsumoForm;
import com.garepas.garepasapp.dto.request.AjusteStockRequest;
import com.garepas.garepasapp.dto.request.DetalleAjusteStockRequest;
import com.garepas.garepasapp.dto.response.InsumoResponse;
import com.garepas.garepasapp.enums.UnidadMedida;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.service.InsumoService;
import com.garepas.garepasapp.util.Formato;
import com.garepas.garepasapp.util.UnidadUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/insumos")
@RequiredArgsConstructor
public class InsumoController {

    private final InsumoService insumoService;
    private final Formato formato;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("insumos", insumoService.listarTodos());
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
        form.setUnidadMedida(insumo.unidadMedida());
        form.setStockActual(formato.limpiar(UnidadUtil.desdeBase(insumo.stockActual(), insumo.unidadMedida())));
        form.setStockMinimo(insumo.stockMinimo() == null
                ? null
                : formato.limpiar(UnidadUtil.desdeBase(insumo.stockMinimo(), insumo.unidadMedida())));
        form.setPrecioCompra(formato.pesos(
                insumo.precioPorGramo().multiply(UnidadUtil.factor(insumo.unidadMedida()))));
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
            insumoService.crear(form.toRequest());
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
            insumoService.actualizar(id, form.toRequest());
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
        try {
            insumoService.eliminar(id);
            redirect.addFlashAttribute("mensaje", "Insumo eliminado.");
        } catch (DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("mensaje",
                    "No se puede eliminar el insumo porque esta en uso en una o mas recetas.");
            redirect.addFlashAttribute("tipo", "danger");
        }
        return "redirect:/insumos";
    }

    @GetMapping("/ajustar-stock")
    public String ajustarStockForm(Model model) {
        AjusteStockForm form = new AjusteStockForm();
        // Una fila vacia inicial para que el formulario muestre los selectores.
        form.getInsumoId().add(null);
        form.getCantidad().add(null);
        model.addAttribute("form", form);
        model.addAttribute("insumos", insumoService.listarActivos());
        return "insumos/ajuste";
    }

    @PostMapping("/ajustar-stock")
    public String ajustarStock(@ModelAttribute("form") AjusteStockForm form,
                               Model model, RedirectAttributes redirect) {
        Map<Long, InsumoResponse> porId = insumoService.listarActivos().stream()
                .collect(Collectors.toMap(InsumoResponse::id, Function.identity()));

        List<DetalleAjusteStockRequest> detalles = form.filasValidas().stream()
                .filter(f -> porId.containsKey(f.insumoId()))
                .map(f -> new DetalleAjusteStockRequest(
                        f.insumoId(),
                        UnidadUtil.aBase(f.cantidad(), porId.get(f.insumoId()).unidadMedida())))
                .toList();

        if (detalles.isEmpty()) {
            model.addAttribute("error", "Agrega al menos un insumo con cantidad distinta de cero.");
            model.addAttribute("insumos", insumoService.listarActivos());
            return "insumos/ajuste";
        }

        try {
            insumoService.ajustarStock(new AjusteStockRequest(detalles));
        } catch (OperacionInvalidaException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("insumos", insumoService.listarActivos());
            return "insumos/ajuste";
        }
        redirect.addFlashAttribute("mensaje", "Stock ajustado para " + detalles.size() + " insumo(s).");
        return "redirect:/insumos";
    }

    private void cargarUnidades(Model model) {
        model.addAttribute("unidades", UnidadMedida.values());
    }
}