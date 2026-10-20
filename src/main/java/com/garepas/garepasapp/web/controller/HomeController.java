package com.garepas.garepasapp.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Ruta raiz: redirige al inicio que corresponde al rol del usuario
 * (mismo destino que el login) para que los enlaces de marca y de las
 * paginas de error no lleven a una URL inexistente.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String inicio(Authentication authentication) {
        boolean esAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return esAdmin ? "redirect:/reportes" : "redirect:/ventas";
    }
}
