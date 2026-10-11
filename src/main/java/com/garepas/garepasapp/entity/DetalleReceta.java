package com.garepas.garepasapp.entity;

import com.garepas.garepasapp.enums.UnidadMedida;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_recetas")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnidadMedida unidadMedida;
}
