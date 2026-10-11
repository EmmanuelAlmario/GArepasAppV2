package com.garepas.garepasapp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recetas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_recetas_nombre", columnNames = "nombre")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(length = 150)
    private String descripcion;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleReceta> detalles = new ArrayList<>();

    /**
     * Insumo que esta receta fabrica (ej.: masa preparada). Si es null, la receta
     * es de producto. Si tiene valor, el precio por gramo de ese insumo se calcula
     * automaticamente como costo de la receta / rendimiento.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_producido_id")
    private Insumo insumoProducido;

    /** Cantidad que rinde un lote, en la unidad base del insumo producido (g / ml / u). */
    @Column(precision = 15, scale = 4)
    private BigDecimal rendimiento;
}
