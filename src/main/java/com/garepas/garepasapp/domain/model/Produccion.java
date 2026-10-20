package com.garepas.garepasapp.domain.model;

import com.garepas.garepasapp.domain.enums.EstadoProduccion;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

  





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

      



    @Column(name = "costo_total", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal costoTotal = BigDecimal.ZERO;

      




    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoProduccion estado = EstadoProduccion.PENDIENTE;

      



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @OneToMany(mappedBy = "produccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleProduccion> detalles = new ArrayList<>();

      
    public boolean esAutomatica() {
        return venta != null;
    }
}
