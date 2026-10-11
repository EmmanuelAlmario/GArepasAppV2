package com.garepas.garepasapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de la aplicacion web: sesion HTTP con formulario de login.
 *
 * Reglas de acceso:
 *  - Publico: login, recursos estaticos y healthcheck.
 *  - VENTAS: punto de venta y jornadas.
 *  - ADMIN: todo lo demas (inventario, produccion, gastos, reportes, usuarios).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/actuator/health",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                // PWA instalable: manifest, service worker y pagina offline.
                                "/manifest.json", "/sw.js", "/offline.html").permitAll()
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/insumos/**", "/productos/**", "/recetas/**",
                                "/producciones/**", "/gastos/**", "/reportes/**").hasRole("ADMIN")
                        // El historial y la eliminacion de ventas son solo para ADMIN (UC23/UC24).
                        .requestMatchers("/ventas/historial").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/ventas/*/eliminar").hasRole("ADMIN")
                        .requestMatchers("/ventas/**", "/jornadas/**").hasAnyRole("ADMIN", "VENTAS")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean esAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                            response.sendRedirect(esAdmin ? "/reportes" : "/ventas");
                        })
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}