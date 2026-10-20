package com.garepas.garepasapp.domain.model;

import com.garepas.garepasapp.domain.enums.CategoriaGasto;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

  






@Entity
@Table(name = "gastos", indexes = {
        @Index(name = "idx_gastos_fecha", columnList = "fecha"),
        @Index(name = "idx_gastos_categoria", columnList = "categoria"),
        @Index(name = "idx_gastos_produccion_id", columnList = "produccion_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaGasto categoria;

    @Column(nullable = false)
    private LocalDateTime fecha;

      



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produccion_id")
    private Produccion produccion;

      
    public boolean esAutomatico() {
        return produccion != null;
    }
}
