package com.garepas.garepasapp.web.controller;

import com.garepas.garepasapp.domain.command.DetalleVentaCommand;
import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.enums.FormaPago;
import com.garepas.garepasapp.domain.enums.MetodoPago;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.service.AbonoService;
import com.garepas.garepasapp.domain.service.ClienteService;
import com.garepas.garepasapp.domain.service.ConfiguracionNegocioService;
import com.garepas.garepasapp.domain.service.JornadaService;
import com.garepas.garepasapp.domain.service.ProductoService;
import com.garepas.garepasapp.domain.service.VentaService;
import com.garepas.garepasapp.shared.Formato;
import com.garepas.garepasapp.web.form.AbonoForm;
import com.garepas.garepasapp.web.form.VentaForm;
import com.garepas.garepasapp.web.view.JornadaResponse;
import com.garepas.garepasapp.web.view.ProductoResponse;
import com.garepas.garepasapp.web.view.VentaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Punto de venta (UC20-UC24): jornada de operacion, registro de ventas,
 * historial paginado (solo ADMIN, ver SecurityConfig) y anulacion con
 * reversion de stock.
 */
@Controller
@RequestMapping("/ventas")
@RequiredArgsConstructor
public class VentaController {

    private static final int TAMANO_PAGINA = 20;

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final JornadaService jornadaService;
    private final ClienteService clienteService;
    private final AbonoService abonoService;
    private final ConfiguracionNegocioService configuracionNegocioService;
    private final Formato formato;

    @GetMapping
    public String pos(Model model) {
        return cargarPos(model, new VentaForm(), List.of());
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute("form") VentaForm form,
                            Model model, RedirectAttributes redirect) {
        List<DetalleVentaCommand> detalles = form.detallesValidos();
        if (detalles.isEmpty()) {
            redirect.addFlashAttribute("mensaje", "Agrega al menos un producto al pedido.");
            redirect.addFlashAttribute("tipo", "warning");
            return "redirect:/ventas";
        }
        try {
            VentaResponse venta = ventaService.registrar(form.toCommand());
            redirect.addFlashAttribute("mensaje",
                    "Venta registrada por " + formato.moneda(venta.total()) + ".");
            // Enlace al comprobante de la venta recien registrada.
            redirect.addFlashAttribute("ultimaVentaId", venta.id());
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

    /**
     * Historial de ventas: busqueda y filtro por estado resueltos en base de
     * datos, con paginacion. Antes se cargaban TODAS las ventas con sus
     * detalles y se filtraba en memoria.
     */
    @GetMapping("/historial")
    public String historial(@RequestParam(required = false) String q,
                            @RequestParam(required = false) EstadoVenta estado,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), TAMANO_PAGINA,
                Sort.by(Sort.Direction.DESC, "fecha"));
        Page<VentaResponse> ventas = ventaService.buscar(q, estado, pageable);

        model.addAttribute("ventas", ventas.getContent());
        model.addAttribute("pagina", ventas);
        model.addAttribute("q", q);
        model.addAttribute("estado", estado);
        return "ventas/historial";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        ventaService.anular(id);
        redirect.addFlashAttribute("mensaje",
                "Venta anulada: se devolvio el stock y dejo de contar en los reportes.");
        return "redirect:/ventas/historial";
    }

    /**
     * Formulario de cobro de una venta a credito: el monto va precargado con
     * el saldo pendiente, asi un cobro total es un solo clic.
     */
    @GetMapping("/{id}/cobrar")
    public String cobrar(@PathVariable Long id, Model model) {
        VentaResponse venta = ventaService.buscarPorId(id);
        if (!venta.pendiente()) {
            model.addAttribute("aviso", "La venta #" + id + " no tiene saldo pendiente.");
        }
        AbonoForm form = new AbonoForm();
        form.setMonto(venta.saldo());
        form.setMetodo(MetodoPago.EFECTIVO);
        model.addAttribute("v", venta);
        model.addAttribute("form", form);
        model.addAttribute("metodos", MetodoPago.values());
        return "ventas/cobrar";
    }

    /** Registra el cobro: baja el saldo de la venta y deja el abono anotado. */
    @PostMapping("/{id}/cobrar")
    public String registrarCobro(@PathVariable Long id, @ModelAttribute("form") AbonoForm form,
                                 BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("v", ventaService.buscarPorId(id));
            model.addAttribute("metodos", MetodoPago.values());
            return "ventas/cobrar";
        }
        try {
            abonoService.registrar(id, form.toCommand());
            redirect.addFlashAttribute("mensaje", "Cobro registrado. El saldo de la venta se actualizo.");
            return "redirect:/ventas/historial";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("v", ventaService.buscarPorId(id));
            model.addAttribute("metodos", MetodoPago.values());
            return "ventas/cobrar";
        }
    }

    /**
     * Comprobante de venta: los datos del negocio, el desglose y el total.
     * Se imprime desde el navegador (el POS tiene un boton "Recibo").
     */
    @GetMapping("/{id}/recibo")
    public String recibo(@PathVariable Long id, Model model) {
        // Se pasa la vista (no la entidad): los detalles ya vienen resueltos
        // y no hay que forzar la sesion de JPA desde la plantilla.
        model.addAttribute("v", ventaService.buscarPorId(id));
        model.addAttribute("config", configuracionNegocioService.obtener());
        model.addAttribute("abonos", abonoService.listarPorVenta(id));
        return "ventas/recibo";
    }

    /**
     * Carga el catalogo, la jornada, los clientes de la cartera y (si hay) el
     * pedido que se estaba armando. El pedido se expone como JSON para que
     * pos-cart.js reconstruya el carrito.
     */
    private String cargarPos(Model model, VentaForm form, List<DetalleVentaCommand> detalles) {
        List<ProductoResponse> productos = productoService.listarActivos().stream()
                .filter(p -> Boolean.TRUE.equals(p.bajoPedido())
                        || (p.stockActual() != null && p.stockActual() > 0))
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
                    fila.put("bajoPedido", Boolean.TRUE.equals(p.bajoPedido()));
                    return fila;
                })
                .filter(Objects::nonNull)
                .toList();

        model.addAttribute("productos", productos);
        model.addAttribute("clientes", clienteService.listarActivos());
        model.addAttribute("formasPago", FormaPago.values());
        model.addAttribute("jornada", jornadaService.activa());
        model.addAttribute("form", form);
        model.addAttribute("carritoInicial", carrito);
        return "ventas/pos";
    }
}
