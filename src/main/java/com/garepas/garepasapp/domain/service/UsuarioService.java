package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.UsuarioCommand;
import com.garepas.garepasapp.domain.enums.Rol;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.domain.model.Usuario;
import com.garepas.garepasapp.domain.repository.UsuarioRepository;
import com.garepas.garepasapp.web.view.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

  











@Service
@RequiredArgsConstructor
public class UsuarioService {

    public static final String USUARIO_RESPALDO = "admin";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioCommand command) {
        if (usuarioRepository.existsByUsernameIgnoreCase(command.username())) {
            throw new RecursoDuplicadoException("Usuario", "usuario", command.username());
        }
        if (command.password() == null || command.password().isBlank()) {
            throw new OperacionInvalidaException("La contrasena es obligatoria");
        }
        Usuario usuario = Usuario.builder()
                .username(command.username())
                .password(passwordEncoder.encode(command.password()))
                .rol(command.rol())
                .activo(command.activo() == null || command.activo())
                .build();
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioCommand command) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));

        if (!usuario.getUsername().equalsIgnoreCase(command.username())
                && usuarioRepository.existsByUsernameIgnoreCase(command.username())) {
            throw new RecursoDuplicadoException("Usuario", "usuario", command.username());
        }

        boolean pierdeAdmin = esAdminActivo(usuario)
                && (command.rol() != Rol.ADMIN || Boolean.FALSE.equals(command.activo()));
        if (pierdeAdmin) {
            verificarQueQuedaUnAdmin();
        }

        usuario.setUsername(command.username());
        if (command.password() != null && !command.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(command.password()));
        }
        usuario.setRol(command.rol());
        if (command.activo() != null) {
            usuario.setActivo(command.activo());
        }
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));

        if (USUARIO_RESPALDO.equalsIgnoreCase(usuario.getUsername())) {
            throw new OperacionInvalidaException(
                    "El usuario de respaldo '" + USUARIO_RESPALDO + "' no se puede eliminar");
        }
        if (esAdminActivo(usuario)) {
            verificarQueQuedaUnAdmin();
        }
        usuarioRepository.deleteById(id);
    }

    private boolean esAdminActivo(Usuario usuario) {
        return usuario.getRol() == Rol.ADMIN && Boolean.TRUE.equals(usuario.getActivo());
    }

      
    private void verificarQueQuedaUnAdmin() {
        long admins = usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN);
        if (admins <= 1) {
            throw new OperacionInvalidaException(
                    "Debe quedar al menos un administrador activo en el sistema");
        }
    }
}
