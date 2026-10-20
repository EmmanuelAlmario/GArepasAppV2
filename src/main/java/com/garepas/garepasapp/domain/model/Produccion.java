package com.garepas.garepasapp.domain.model;

import com.garepas.garepasapp.domain.enums.EstadoProduccion;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Produccion de un producto a partir de su receta.
 *
 * <p>La venta que la origino (en el caso de los productos bajo pedido) es una
 * relacion real: antes era un {@code ventaId} suelto sin clave foranea.
 */
@Entity
@Table(name = "producciones", indexes = {
        @Index(name = "idx_producciones_fecha", columnList = "fecha"),
        @Index(name = "idx_producciones_venta_id", columnList = "venta_id"),
        @Index(name = "idx_producciones_producto_id", columnList = "producto_id")
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
     * Costo total de materia prima consumida en esta produccion.
     * Sumatoria de (cantidadUsada * insumo.precioPorUnidadBase) al momento del registro.
     */
    @Column(name = "costo_total", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal costoTotal = BigDecimal.ZERO;

    /**
     * Estado de la produccion:
     * - PENDIENTE: hay insumos insuficientes, no se descontó stock ni se creó gasto.
     * - COMPLETADA: todos los insumos son suficientes, stock descontado y gasto creado.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoProduccion estado = EstadoProduccion.PENDIENTE;

    /**
     * Venta que origino la produccion cuando es automatica (producto bajo pedido).
     * null = produccion registrada manualmente desde el modulo Producciones.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @OneToMany(mappedBy = "produccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleProduccion> detalles = new ArrayList<>();

    /** true si la produccion fue generada automaticamente por una venta. */
    public boolean esAutomatica() {
        return venta != null;
    }
}
