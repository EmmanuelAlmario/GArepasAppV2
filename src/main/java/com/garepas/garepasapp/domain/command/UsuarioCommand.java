package com.garepas.garepasapp.domain.command;

import com.garepas.garepasapp.domain.enums.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioCommand(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 50, message = "El usuario no puede superar los 50 caracteres")
        String username,
        @Size(min = 6, max = 100, message = "La contrasena debe tener entre 6 y 100 caracteres")
        String password,
        @NotNull(message = "El rol es obligatorio")
        Rol rol,
        Boolean activo
) {
}
