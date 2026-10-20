package com.garepas.garepasapp.web.controller;

import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.model.Abono;
import com.garepas.garepasapp.domain.model.Cliente;
import com.garepas.garepasapp.domain.model.Venta;
import com.garepas.garepasapp.domain.service.AbonoService;
import com.garepas.garepasapp.domain.service.ClienteService;
import com.garepas.garepasapp.domain.service.VentaService;
import com.garepas.garepasapp.shared.Formato;
import com.garepas.garepasapp.web.form.AbonoForm;
import com.garepas.garepasapp.web.form.ClienteForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clientes y cartera: quien fia, cuanto debe y el estado de cuenta de cada
 * uno. Ver tambien {@link AbonoController}, que registra los cobros.
 */
@Controller
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final VentaService ventaService;
    private final AbonoService abonoService;
    private final Formato formato;

    @GetMapping
    public String listar(Model model) {
        List<Cliente> clientes = clienteService.listarTodos();
        Map<Long, BigDecimal> saldos = new LinkedHashMap<>();
        for (Cliente cliente : clientes) {
            saldos.put(cliente.getId(), clienteService.saldoDe(cliente.getId()));
        }
        model.addAttribute("clientes", clientes);
        model.addAttribute("saldos", saldos);
        model.addAttribute("totalPorCobrar", saldos.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return "clientes/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new ClienteForm());
        return "clientes/form";
    }

    @PostMapping
    public String crear(@ModelAttribute("form") ClienteForm form,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("form", form);
            return "clientes/form";
        }
        clienteService.crear(form.toCommand());
        redirect.addFlashAttribute("mensaje", "Cliente creado correctamente.");
        return "redirect:/clientes";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Cliente cliente = clienteService.buscarPorId(id);
        ClienteForm form = new ClienteForm();
        form.setId(cliente.getId());
        form.setNombre(cliente.getNombre());
        form.setTelefono(cliente.getTelefono());
        form.setDireccion(cliente.getDireccion());
        form.setNotas(cliente.getNotas());
        form.setActivo(cliente.getActivo());
        model.addAttribute("form", form);
        return "clientes/form";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id, @ModelAttribute("form") ClienteForm form,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("form", form);
            return "clientes/form";
        }
        clienteService.actualizar(id, form.toCommand());
        redirect.addFlashAttribute("mensaje", "Cliente actualizado correctamente.");
        return "redirect:/clientes";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            clienteService.eliminar(id);
            redirect.addFlashAttribute("mensaje", "Cliente eliminado.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("mensaje", ex.getMessage());
            redirect.addFlashAttribute("tipo", "warning");
        }
        return "redirect:/clientes";
    }

    /** Estado de cuenta: ventas del cliente y abonos recibidos. */
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Cliente cliente = clienteService.buscarPorId(id);
        model.addAttribute("cliente", cliente);
        model.addAttribute("saldo", clienteService.saldoDe(id));
        model.addAttribute("ventas", ventaService.ventasDeCliente(id));
        model.addAttribute("abonos", abonoService.listarPorCliente(id));
        model.addAttribute("form", new AbonoForm());
        return "clientes/detalle";
    }
}
