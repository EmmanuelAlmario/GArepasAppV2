package com.garepas.garepasapp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

  






@Entity
@Table(name = "ajustes_inventario", indexes = {
        @Index(name = "idx_ajustes_insumo_id", columnList = "insumo_id"),
        @Index(name = "idx_ajustes_fecha", columnList = "fecha")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjusteInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

      
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal delta;

      
    @Column(name = "stock_resultante", nullable = false, precision = 15, scale = 4)
    private BigDecimal stockResultante;

      
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 150)
    private String motivo;

      
    @Column(nullable = false, length = 60)
    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
