package com.garepas.garepasapp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_producciones", indexes = {
        @Index(name = "idx_det_prod_produccion_id", columnList = "produccion_id"),
        @Index(name = "idx_det_prod_insumo_id", columnList = "insumo_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleProduccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produccion_id", nullable = false)
    private Produccion produccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal cantidadRequerida;

    @Column(precision = 15, scale = 4)
    private BigDecimal cantidadUsada;

      



    @Column(name = "precio_unidad_base_snapshot", precision = 15, scale = 6)
    private BigDecimal precioUnidadBaseSnapshot;

    @Column(precision = 15, scale = 4)
    private BigDecimal costoLinea;

    @Column(nullable = false)
    private Boolean suficiente;
}
