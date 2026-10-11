package com.garepas.garepasapp.entity;

import com.garepas.garepasapp.enums.EstadoProduccion;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "producciones", indexes = {
        @Index(name = "idx_producciones_fecha", columnList = "fecha")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /**
     * Costo total de materia prima consumida en esta producción.
     * Sumatoria de (cantidadUsada * insumo.precioPorGramo) al momento del registro.
     */
    @Column(name = "costo_total", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal costoTotal = BigDecimal.ZERO;

    /**
     * Estado de la producción:
     * - PENDIENTE: hay insumos insuficientes, no se descontó stock ni se creó gasto.
     * - COMPLETADA: todos los insumos son suficientes, stock descontado y gasto creado.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoProduccion estado = EstadoProduccion.PENDIENTE;

    @OneToMany(mappedBy = "produccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleProduccion> detalles = new ArrayList<>();
}
