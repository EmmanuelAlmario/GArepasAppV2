package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.model.Usuario;
import com.garepas.garepasapp.domain.enums.Rol;

public record UsuarioResponse(
        Long id,
        String username,
        Rol rol,
        Boolean activo
) {
    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getRol(),
                usuario.getActivo());
    }
}