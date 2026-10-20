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
                                  
                                  
                                  
                                  
                                "/vendor/**",
                                  
                                "/manifest.json", "/sw.js", "/offline.html").permitAll()
                          
                          
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/insumos/**", "/productos/**", "/recetas/**",
                                "/producciones/**", "/gastos/**", "/reportes/**").hasRole("ADMIN")
                          
                        .requestMatchers("/configuracion/**").hasRole("ADMIN")
                          
                          
                        .requestMatchers("/clientes/nuevo", "/clientes/*/editar", "/clientes/*/eliminar")
                        .hasRole("ADMIN")
                        .requestMatchers("/clientes/**", "/abonos/**").hasAnyRole("ADMIN", "VENTAS")
                          
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