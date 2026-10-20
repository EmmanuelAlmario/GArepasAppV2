package com.garepas.garepasapp.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

  




@Controller
public class HomeController {

    @GetMapping("/")
    public String inicio(Authentication authentication) {
        boolean esAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return esAdmin ? "redirect:/reportes" : "redirect:/ventas";
    }
}
