package com.garepas.garepasapp.domain.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

  


public record ClienteCommand(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
        String nombre,
        @Size(max = 30, message = "El telefono no puede superar los 30 caracteres")
        String telefono,
        @Size(max = 150, message = "La direccion no puede superar los 150 caracteres")
        String direccion,
        @Size(max = 250, message = "Las notas no pueden superar los 250 caracteres")
        String notas,
        Boolean activo
) {
}
