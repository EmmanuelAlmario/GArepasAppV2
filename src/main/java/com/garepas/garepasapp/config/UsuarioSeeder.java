package com.garepas.garepasapp.config;

import com.garepas.garepasapp.entity.Usuario;
import com.garepas.garepasapp.enums.Rol;
import com.garepas.garepasapp.repository.UsuarioRepository;
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

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        upsert("admin", adminPassword, Rol.ADMIN);
    }

    /**
     * Crea el usuario si no existe, o actualiza su contraseña/rol si sí existe.
     * Así, cambiar ADMIN_PASSWORD / VENTAS_PASSWORD en el entorno basta para
     * actualizar la contraseña en un redeploy (no solo al primer arranque).
     */
    private void upsert(String username, String password, Rol rol) {
        var usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseGet(() -> Usuario.builder().username(username).build());
        boolean nuevo = usuario.getId() == null;
        if (nuevo) {
            usuario.setActivo(true);
        }
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(rol);
        usuarioRepository.save(usuario);
        if (nuevo) {
            log.info("Usuario por defecto creado: '{}' con rol {}", username, rol);
        } else {
            log.info("Contraseña de '{}' actualizada desde el entorno.", username);
        }
    }
}