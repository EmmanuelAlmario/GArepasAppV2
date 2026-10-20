package com.garepas.garepasapp.domain.repository;

import com.garepas.garepasapp.domain.enums.Rol;
import com.garepas.garepasapp.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    /** Numero de administradores activos: el sistema no puede quedarse sin ninguno. */
    long countByRolAndActivoTrue(Rol rol);
}