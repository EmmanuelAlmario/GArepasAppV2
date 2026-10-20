package com.garepas.garepasapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
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
 *  - Publico: login, recursos estaticos, healthcheck y lectura GET del menu
 *    (/api/public/**, con CORS restringido en CorsConfig).
 *  - VENTAS: punto de venta y jornadas.
 *  - ADMIN: todo lo demas (inventario, produccion, gastos, reportes, usuarios).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/actuator/health",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                // Librerias servidas desde el propio servidor (Bootstrap,
                                // Bootstrap Icons, Chart.js): sin esto el navegador
                                // recibia el HTML del login en vez del CSS y la aplicacion
                                // se veía sin estilos.
                                "/vendor/**",
                                // PWA instalable: manifest, service worker y pagina offline.
                                "/manifest.json", "/sw.js", "/offline.html").permitAll()
                        // API publica del menu: solo lectura, sin sesion. Cualquier otro
                        // metodo sobre /api/public cae en las reglas de abajo (no publicas).
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/insumos/**", "/productos/**", "/recetas/**",
                                "/producciones/**", "/gastos/**", "/reportes/**").hasRole("ADMIN")
                        // Configuracion del negocio: solo ADMIN.
                        .requestMatchers("/configuracion/**").hasRole("ADMIN")
                        // Alta, edicion y baja de clientes: solo ADMIN.
                        // (Consultar la cartera y cobrar es tarea de los registradores.)
                        .requestMatchers("/clientes/nuevo", "/clientes/*/editar", "/clientes/*/eliminar")
                        .hasRole("ADMIN")
                        .requestMatchers("/clientes/**", "/abonos/**").hasAnyRole("ADMIN", "VENTAS")
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