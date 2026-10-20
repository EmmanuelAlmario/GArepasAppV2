package com.garepas.garepasapp.domain.service;

import com.garepas.garepasapp.domain.command.UsuarioCommand;
import com.garepas.garepasapp.domain.enums.Rol;
import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.model.Usuario;
import com.garepas.garepasapp.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

  


class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenAnswer(inv -> "hash:" + inv.getArgument(0));
        usuarioService = new UsuarioService(usuarioRepository, encoder);
    }

    private Usuario usuario(Long id, String username, Rol rol, boolean activo) {
        return Usuario.builder().id(id).username(username).password("hash").rol(rol).activo(activo).build();
    }

    @Test
    void noSePuedeEliminarElUsuarioDeRespaldo() {
        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario(1L, "admin", Rol.ADMIN, true)));

        assertThrows(OperacionInvalidaException.class, () -> usuarioService.eliminar(1L));
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    void noSePuedeEliminarElUltimoAdministradorActivo() {
        when(usuarioRepository.findById(2L))
                .thenReturn(Optional.of(usuario(2L, "otro-admin", Rol.ADMIN, true)));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(1L);

        assertThrows(OperacionInvalidaException.class, () -> usuarioService.eliminar(2L));
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    void noSePuedeDejarSinAdministradoresDesactivandoAlUltimo() {
        when(usuarioRepository.findById(2L))
                .thenReturn(Optional.of(usuario(2L, "otro-admin", Rol.ADMIN, true)));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(1L);

        assertThrows(OperacionInvalidaException.class, () -> usuarioService.actualizar(2L,
                new UsuarioCommand("otro-admin", null, Rol.ADMIN, false)));
    }

    @Test
    void noSePuedeDejarSinAdministradoresDegradandoAlUltimo() {
        when(usuarioRepository.findById(2L))
                .thenReturn(Optional.of(usuario(2L, "otro-admin", Rol.ADMIN, true)));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(1L);

        assertThrows(OperacionInvalidaException.class, () -> usuarioService.actualizar(2L,
                new UsuarioCommand("otro-admin", null, Rol.VENTAS, true)));
    }

    @Test
    void sePuedeEliminarUnAdministradorCuandoHayOtros() {
        when(usuarioRepository.findById(2L))
                .thenReturn(Optional.of(usuario(2L, "otro-admin", Rol.ADMIN, true)));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(2L);

        usuarioService.eliminar(2L);

        verify(usuarioRepository).deleteById(2L);
    }

    @Test
    void alEditarSinContrasenaSeConservaLaQueTenia() {
        Usuario admin = usuario(1L, "admin", Rol.ADMIN, true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(2L);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.actualizar(1L, new UsuarioCommand("admin", null, Rol.ADMIN, true));

        assertEquals("hash", admin.getPassword());
    }
}
