package com.garepas.garepasapp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Ingrediente de una receta.
 *
 * <p>La cantidad se almacena siempre en la unidad BASE del insumo (g, ml, u).
 * Antes esta tabla guardaba ademas una columna {@code unidad_medida} que
 * duplicaba (y a veces contradecía) la unidad del insumo: la receta usaba
 * "120" + "KILOGRAMO" mientras el insumo era "120 g", y el descuento de stock
 * se calculaba enbase con la unidad equivocada.
 *
 * <p>La unidad se deriva ahora del insumo: {@code insumo.unidadBase()}.
 */
@Entity
@Table(name = "detalle_recetas", indexes = {
        @Index(name = "idx_detalle_receta_receta_id", columnList = "receta_id"),
        @Index(name = "idx_detalle_receta_insumo_id", columnList = "insumo_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleReceta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    /** Cantidad del ingrediente en la unidad BASE del insumo. */
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal cantidad;
}
