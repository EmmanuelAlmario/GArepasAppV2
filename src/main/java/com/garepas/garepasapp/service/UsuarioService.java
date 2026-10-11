package com.garepas.garepasapp.service;

import com.garepas.garepasapp.dto.request.UsuarioRequest;
import com.garepas.garepasapp.dto.response.UsuarioResponse;
import com.garepas.garepasapp.entity.Usuario;
import com.garepas.garepasapp.exception.OperacionInvalidaException;
import com.garepas.garepasapp.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.exception.RecursoNoEncontradoException;
import com.garepas.garepasapp.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponse::desde)
                .toList();
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new OperacionInvalidaException("La contraseña es obligatoria para crear un usuario.");
        }
        if (usuarioRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new RecursoDuplicadoException("Usuario", "username", request.username());
        }
        Usuario usuario = Usuario.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .activo(request.activo() == null ? Boolean.TRUE : request.activo())
                .build();
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        if (!usuario.getUsername().equalsIgnoreCase(request.username()) &&
                usuarioRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new RecursoDuplicadoException("Usuario", "username", request.username());
        }
        usuario.setUsername(request.username());
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.password()));
        }
        usuario.setRol(request.rol());
        usuario.setActivo(request.activo() == null ? usuario.getActivo() : request.activo());
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        if ("admin".equalsIgnoreCase(usuario.getUsername())) {
            throw new OperacionInvalidaException("No se puede eliminar el usuario admin de respaldo.");
        }
        usuarioRepository.delete(usuario);
    }
}