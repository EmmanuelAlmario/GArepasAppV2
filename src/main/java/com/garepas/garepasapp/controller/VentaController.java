package com.garepas.garepasapp.controller;

import com.garepas.garepasapp.dto.form.VentaForm;
import com.garepas.garepasapp.dto.request.DetalleVentaRequest;
import com.garepas.garepasapp.dto.response.JornadaResponse;
import com.garepas.garepasapp.dto.response.ProductoResponse;
import com.garepas.garepasapp.dto.response.VentaResponse;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.service.JornadaService;
import com.garepas.garepasapp.service.ProductoService;
import com.garepas.garepasapp.service.VentaService;
import com.garepas.garepasapp.util.Formato;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Punto de venta (UC20-UC24): jornada de operacion, registro de ventas,
 * historial (solo ADMIN, ver SecurityConfig) y eliminacion con reversion de stock.
 */
@Controller
@RequestMapping("/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final JornadaService jornadaService;
    private final Formato formato;

    @GetMapping
    public String pos(Model model) {
        return cargarPos(model, new VentaForm(), List.of());
    }

    @PostMapping
    public String registrar(@ModelAttribute("form") VentaForm form, Model model, RedirectAttributes redirect) {
        List<DetalleVentaRequest> detalles = form.detallesValidos();
        if (detalles.isEmpty()) {
            redirect.addFlashAttribute("mensaje", "Agrega al menos un producto al pedido.");
            redirect.addFlashAttribute("tipo", "warning");
            return "redirect:/ventas";
        }
        try {
            VentaResponse venta = ventaService.registrar(form.toRequest());
            redirect.addFlashAttribute("mensaje",
                    "Venta registrada por " + formato.moneda(venta.total()) + ".");
        } catch (OperacionInvalidaException | RecursoNoEncontradoException ex) {
            // Se vuelve a mostrar el punto de venta con el pedido y el cliente
            // conservados para que el usuario pueda corregir sin rearmar todo.
            model.addAttribute("error", ex.getMessage());
            return cargarPos(model, form, detalles);
        }
        return "redirect:/ventas";
    }

    @PostMapping("/jornada/abrir")
    public String abrirJornada(Authentication auth, RedirectAttributes redirect) {
        try {
            jornadaService.abrir(auth != null ? auth.getName() : null);
            redirect.addFlashAttribute("mensaje",
                    "Dia abierto. Las ventas de ahora en adelante se contabilizan en esta jornada.");
        } catch (OperacionInvalidaException ex) {
            redirect.addFlashAttribute("mensaje", ex.getMessage());
            redirect.addFlashAttribute("tipo", "warning");
        }
        return "redirect:/ventas";
    }

    @PostMapping("/jornada/cerrar")
    public String cerrarJornada(RedirectAttributes redirect) {
        JornadaResponse activa = jornadaService.activa();
        if (activa == null) {
            redirect.addFlashAttribute("mensaje", "No hay una jornada abierta.");
            redirect.addFlashAttribute("tipo", "warning");
            return "redirect:/ventas";
        }
        JornadaResponse arqueo = jornadaService.cerrar(activa.id());
        redirect.addFlashAttribute("mensaje",
                "Dia cerrado — " + arqueo.nroVentas() + " venta(s) · total " + formato.moneda(arqueo.totalVentas()) + ".");
        return "redirect:/ventas";
    }

    @GetMapping("/historial")
    public String historial(@RequestParam(required = false) String q, Model model) {
        List<VentaResponse> ventas = ventaService.listarTodas();
        if (q != null && !q.isBlank()) {
            String texto = q.trim().toLowerCase();
            ventas = ventas.stream()
                    .filter(v -> String.valueOf(v.id()).contains(texto)
                            || (v.nombreCliente() != null && v.nombreCliente().toLowerCase().contains(texto))
                            || v.detalles().stream().anyMatch(d -> d.productoNombre() != null
                                    && d.productoNombre().toLowerCase().contains(texto)))
                    .toList();
        }
        model.addAttribute("ventas", ventas);
        model.addAttribute("q", q);
        return "ventas/historial";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        ventaService.eliminar(id);
        redirect.addFlashAttribute("mensaje", "Venta eliminada y stock revertido.");
        return "redirect:/ventas/historial";
    }

    /**
     * Carga el catalogo, la jornada y (si hay) el pedido que se estaba armando.
     * El pedido se expone como JSON para que pos-cart.js reconstruya el carrito.
     */
    private String cargarPos(Model model, VentaForm form, List<DetalleVentaRequest> detalles) {
        List<ProductoResponse> productos = productoService.listarActivos().stream()
                .filter(p -> p.stockActual() != null && p.stockActual() > 0)
                .toList();

        Map<Long, ProductoResponse> porId = new LinkedHashMap<>();
        productos.forEach(p -> porId.put(p.id(), p));

        List<Map<String, Object>> carrito = detalles.stream()
                .map(d -> {
                    ProductoResponse p = porId.get(d.productoId());
                    if (p == null) {
                        return null;
                    }
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("id", p.id());
                    fila.put("nombre", p.nombre());
                    fila.put("precio", p.precioVenta());
                    fila.put("cantidad", d.cantidad());
                    fila.put("stock", p.stockActual());
                    return fila;
                })
                .filter(Objects::nonNull)
                .toList();

        model.addAttribute("productos", productos);
        model.addAttribute("jornada", jornadaService.activa());
        model.addAttribute("form", form);
        model.addAttribute("carritoInicial", carrito);
        return "ventas/pos";
    }
}
