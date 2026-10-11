package com.garepas.garepasapp.entity;

import com.garepas.garepasapp.enums.UnidadMedida;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "insumos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_insumos_nombre", columnNames = "nombre")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String categoria;

    @Column(nullable = false, length = 30)
    private String marca;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal stockActual;

    /** Umbral opcional para alerta de stock bajo. null = sin alerta. */
    @Column(precision = 15, scale = 4)
    private BigDecimal stockMinimo;

    /**
     * Precio por unidad base (gramo o mililitro o unidad). Se guarda con
     * precisión fina porque puede ser una fracción pequeña ($0.0125/g).
     */
    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal precioPorGramo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnidadMedida unidadMedida;

    @Column(nullable = false)
    private Boolean activo;

    @Version
    private Long version;
}
