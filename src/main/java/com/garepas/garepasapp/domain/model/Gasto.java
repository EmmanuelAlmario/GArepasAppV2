package com.garepas.garepasapp.domain.model;

import com.garepas.garepasapp.domain.enums.CategoriaGasto;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Egreso de dinero.
 *
 * <p>Los gastos de materia prima que genera una produccion quedan enlazados a
 * ella por relacion real (antes era un {@code produccionId} suelto sin clave
 * foranea), lo que permite borrarlos en cascada al revertir la produccion.
 */
@Entity
@Table(name = "gastos", indexes = {
        @Index(name = "idx_gastos_fecha", columnList = "fecha"),
        @Index(name = "idx_gastos_categoria", columnList = "categoria"),
        @Index(name = "idx_gastos_produccion_id", columnList = "produccion_id")
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
     * Produccion que origino este gasto (categoria MATERIA_PRIMA).
     * null = gasto manual registrado desde el modulo de gastos.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produccion_id")
    private Produccion produccion;

    /** true si el gasto fue generado por una produccion (no se edita a mano). */
    public boolean esAutomatico() {
        return produccion != null;
    }
}
