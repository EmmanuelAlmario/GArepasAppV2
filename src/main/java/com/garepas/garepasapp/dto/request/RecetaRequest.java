package com.garepas.garepasapp.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record RecetaRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre no puede superar 50 caracteres")
        String nombre,

        @Size(max = 150, message = "La descripción no puede superar 150 caracteres")
        String descripcion,

        @NotEmpty(message = "La receta debe tener al menos un detalle")
        @Valid
        List<DetalleRecetaRequest> detalles
) {}
