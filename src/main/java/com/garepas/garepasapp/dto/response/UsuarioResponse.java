package com.garepas.garepasapp.dto.response;

import com.garepas.garepasapp.entity.Usuario;
import com.garepas.garepasapp.enums.Rol;

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