package com.garepas.garepasapp.dto.request;

import com.garepas.garepasapp.enums.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(

        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 50, message = "El usuario no puede superar 50 caracteres")
        String username,

        @Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        Boolean activo
) {}