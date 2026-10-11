package com.garepas.garepasapp.entity;

import com.garepas.garepasapp.enums.CategoriaGasto;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gastos", indexes = {
        @Index(name = "idx_gastos_fecha", columnList = "fecha"),
        @Index(name = "idx_gastos_categoria", columnList = "categoria")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaGasto categoria;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /**
     * Referencia opcional a una Produccion que originó este gasto (categoría MATERIA_PRIMA).
     * Se usa para eliminar el gasto en cascada si la producción se revierte.
     * Se guarda como id simple (no relación) para simplicidad y evitar ciclos.
     */
    @Column(name = "produccion_id")
    private Long produccionId;
}
