package com.garepas.garepasapp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

  










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

      
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal cantidad;
}
