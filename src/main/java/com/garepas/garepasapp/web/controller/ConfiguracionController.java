package com.garepas.garepasapp.web.controller;

import com.garepas.garepasapp.domain.model.ConfiguracionNegocio;
import com.garepas.garepasapp.domain.service.ConfiguracionNegocioService;
import com.garepas.garepasapp.web.form.ConfiguracionForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

  



@Controller
@RequestMapping("/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionNegocioService service;

    @GetMapping
    public String editar(Model model) {
        ConfiguracionNegocio config = service.obtener();
        ConfiguracionForm form = new ConfiguracionForm();
        form.setNombreComercial(config.getNombreComercial());
        form.setIdentificacionFiscal(config.getIdentificacionFiscal());
        form.setTelefono(config.getTelefono());
        form.setDireccion(config.getDireccion());
        form.setImpuestoPorcentaje(config.getImpuestoPorcentaje());
        form.setMensajeRecibo(config.getMensajeRecibo());
        form.setLogoUrl(config.getLogoUrl());
        model.addAttribute("form", form);
        return "configuracion/form";
    }

    @PostMapping
    public String guardar(@ModelAttribute("form") ConfiguracionForm form,
                          BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("form", form);
            return "configuracion/form";
        }
        service.guardar(form.toCommand());
        redirect.addFlashAttribute("mensaje", "Configuracion guardada.");
        return "redirect:/configuracion";
    }
}
