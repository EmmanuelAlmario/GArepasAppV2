package com.garepas.garepasapp.config.seed;

import com.garepas.garepasapp.domain.enums.Rol;
import com.garepas.garepasapp.domain.model.Usuario;
import com.garepas.garepasapp.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

  












@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class UsuarioSeeder implements CommandLineRunner {

    public static final String USUARIO_RESPALDO = "admin";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByUsernameIgnoreCase(USUARIO_RESPALDO).isPresent()) {
            log.info("Usuario de respaldo '{}' ya existe: se conserva su clave actual.", USUARIO_RESPALDO);
            return;
        }
        usuarioRepository.save(Usuario.builder()
                .username(USUARIO_RESPALDO)
                .password(passwordEncoder.encode(adminPassword))
                .rol(Rol.ADMIN)
                .activo(true)
                .build());
        log.info("Usuario de respaldo creado: '{}' con rol ADMIN.", USUARIO_RESPALDO);
    }
}
