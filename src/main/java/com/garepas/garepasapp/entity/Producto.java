package com.garepas.garepasapp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "productos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_productos_nombre", columnNames = "nombre")
}, indexes = {
        @Index(name = "idx_productos_receta_id", columnList = "receta_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false)
    private Integer stockActual;

    /** Umbral opcional para alerta de stock bajo. null = sin alerta. */
    @Column
    private Integer stockMinimo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id")
    private Receta receta;

    @Column(nullable = false)
    private Boolean activo;

    @Version
    private Long version;
}
