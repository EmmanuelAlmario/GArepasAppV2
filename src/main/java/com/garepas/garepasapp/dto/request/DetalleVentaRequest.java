package com.garepas.garepasapp.dto.request;

import jakarta.validation.constraints.*;

public record DetalleVentaRequest(

        @NotNull(message = "El id del producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad
) {}
