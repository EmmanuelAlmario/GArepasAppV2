package com.garepas.garepasapp.dto.request;

import com.garepas.garepasapp.enums.UnidadMedida;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record InsumoRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String nombre,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 30, message = "La categoría no puede superar 30 caracteres")
        String categoria,

        @NotBlank(message = "La marca es obligatoria")
        @Size(max = 30, message = "La marca no puede superar 30 caracteres")
        String marca,

        @NotNull(message = "El stock actual es obligatorio")
        @DecimalMin(value = "0.0", message = "El stock no puede ser negativo")
        BigDecimal stockActual,

        @DecimalMin(value = "0.0", message = "El stock mínimo no puede ser negativo")
        BigDecimal stockMinimo,

        @NotNull(message = "El precio por gramo es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a cero")
        BigDecimal precioPorGramo,

        @NotNull(message = "La unidad de medida es obligatoria")
        UnidadMedida unidadMedida,

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {}
