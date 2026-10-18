package com.garepas.garepasapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS restringido a la API publica del menu (/api/public/**).
 *
 * - Solo los origenes listados en CORS_ORIGINS (lista separada por comas).
 *   Si la variable esta vacia, ningun origen cruzado recibe cabeceras CORS
 *   y el navegador bloquea la lectura: el valor por defecto es denegar.
 * - Solo metodo GET, sin credenciales (ni cookies ni Authorization).
 * - No aplica al resto de la aplicacion (Thymeleaf y /api/** privado).
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") List<String> origenesPermitidos) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenesPermitidos.stream()
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET"));
        config.setAllowedHeaders(List.of("Accept", "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/public/**", config);
        return fuente;
    }
}
