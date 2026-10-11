package com.garepas.garepasapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Muestra el formulario de inicio de sesion.
 * El POST /login lo procesa Spring Security (ver SecurityConfig).
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}