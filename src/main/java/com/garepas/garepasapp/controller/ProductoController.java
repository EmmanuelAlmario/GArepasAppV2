package com.garepas.garepasapp.controller;

import com.garepas.garepasapp.dto.form.ProductoForm;
import com.garepas.garepasapp.dto.response.ProductoResponse;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.service.CostoService;
import com.garepas.garepasapp.service.ProductoService;
import com.garepas.garepasapp.service.RecetaService;
import com.garepas.garepasapp.util.Formato;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Controller
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    private static final BigDecimal MARGEN_SUGERIDO = new BigDecimal("0.40");

    private final ProductoService productoService;
    private final RecetaService recetaService;
    private final CostoService costoService;
    private final Formato formato;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", productoService.listarTodos());
        return "productos/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new ProductoForm());
        cargarRecetas(model);
        return "productos/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        ProductoResponse producto = productoService.buscarPorId(id);
        ProductoForm form = new ProductoForm();
        form.setId(producto.id());
        form.setNombre(producto.nombre());
        form.setStockActual(producto.stockActual());
        form.setStockMinimo(producto.stockMinimo());
        form.setPrecioVenta(formato.pesos(producto.precioVenta()));
        form.setRecetaId(producto.recetaId());
        form.setBajoPedido(producto.bajoPedido());
        form.setActivo(producto.activo());
        model.addAttribute("form", form);
        cargarRecetas(model);
        return "productos/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") ProductoForm form,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            cargarRecetas(model);
            return "productos/form";
        }
        try {
            productoService.crear(form.toRequest());
        } catch (RecursoDuplicadoException | OperacionInvalidaException ex) {
            model.addAttribute("error", ex.getMessage());
            cargarRecetas(model);
            return "productos/form";
        }
        redirect.addFlashAttribute("mensaje", "Producto creado correctamente.");
        return "redirect:/productos";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("form") ProductoForm form,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            cargarRecetas(model);
            return "productos/form";
        }
        try {
            productoService.actualizar(id, form.toRequest());
        } catch (RecursoDuplicadoException | OperacionInvalidaException ex) {
            model.addAttribute("error", ex.getMessage());
            cargarRecetas(model);
            return "productos/form";
        }
        redirect.addFlashAttribute("mensaje", "Producto actualizado correctamente.");
        return "redirect:/productos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            productoService.eliminar(id);
            redirect.addFlashAttribute("mensaje", "Producto eliminado.");
        } catch (DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("mensaje",
                    "No se puede eliminar el producto porque tiene ventas o producciones asociadas.");
            redirect.addFlashAttribute("tipo", "danger");
        }
        return "redirect:/productos";
    }

    /**
     * Calcula el precio sugerido aplicando un recargo del 40% sobre el costo
     * de la receta, y devuelve el formulario con el precio propuesto (UC10).
     * No guarda: el usuario debe pulsar Guardar.
     */
    @PostMapping("/sugerir-precio")
    public String sugerirPrecio(@ModelAttribute("form") ProductoForm form, Model model) {
        cargarRecetas(model);
        if (form.getRecetaId() == null) {
            model.addAttribute("aviso", "Selecciona una receta para calcular el costo y sugerir el precio.");
            return "productos/form";
        }
        BigDecimal costo = costoService.calcularCostoReceta(form.getRecetaId()).costoTotal();
        if (costo.compareTo(BigDecimal.ZERO) <= 0) {
            model.addAttribute("aviso", "La receta seleccionada no tiene costo calculable (revisa sus insumos).");
            return "productos/form";
        }
        BigDecimal sugerido = costo.multiply(BigDecimal.ONE.add(MARGEN_SUGERIDO))
                .setScale(0, RoundingMode.HALF_UP);
        form.setPrecioVenta(sugerido);
        model.addAttribute("aviso", "Precio sugerido con un recargo del 40% sobre un costo de $"
                + costo.setScale(0, RoundingMode.HALF_UP) + ". Aún no se ha guardado.");
        return "productos/form";
    }

    private void cargarRecetas(Model model) {
        model.addAttribute("recetas", recetaService.listarTodas());
    }
}