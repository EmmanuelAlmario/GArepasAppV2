package com.garepas.garepasapp.controller;

import com.garepas.garepasapp.dto.form.UsuarioForm;
import com.garepas.garepasapp.dto.response.UsuarioResponse;
import com.garepas.garepasapp.enums.Rol;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gestion de usuarios (UC33). Solo accesible para ADMIN.
 */
@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        return "usuarios/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new UsuarioForm());
        model.addAttribute("roles", Rol.values());
        return "usuarios/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        UsuarioResponse usuario = usuarioService.listar().stream()
                .filter(u -> u.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new com.garepas.garepasapp.exception.RecursoNoEncontradoException("Usuario", id));
        UsuarioForm form = new UsuarioForm();
        form.setId(usuario.id());
        form.setUsername(usuario.username());
        form.setRol(usuario.rol());
        form.setActivo(usuario.activo());
        model.addAttribute("form", form);
        model.addAttribute("roles", Rol.values());
        return "usuarios/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") UsuarioForm form,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            result.rejectValue("password", "password.vacia", "La contrasena es obligatoria para crear un usuario.");
        }
        if (result.hasErrors()) {
            model.addAttribute("roles", Rol.values());
            return "usuarios/form";
        }
        try {
            usuarioService.crear(form.toRequest());
        } catch (RecursoDuplicadoException | OperacionInvalidaException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("roles", Rol.values());
            return "usuarios/form";
        }
        redirect.addFlashAttribute("mensaje", "Usuario creado correctamente.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("form") UsuarioForm form,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("roles", Rol.values());
            return "usuarios/form";
        }
        try {
            usuarioService.actualizar(id, form.toRequest());
        } catch (RecursoDuplicadoException | OperacionInvalidaException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("roles", Rol.values());
            return "usuarios/form";
        }
        redirect.addFlashAttribute("mensaje", "Usuario actualizado correctamente.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            usuarioService.eliminar(id);
            redirect.addFlashAttribute("mensaje", "Usuario eliminado.");
        } catch (OperacionInvalidaException ex) {
            redirect.addFlashAttribute("mensaje", ex.getMessage());
            redirect.addFlashAttribute("tipo", "warning");
        }
        return "redirect:/usuarios";
    }
}