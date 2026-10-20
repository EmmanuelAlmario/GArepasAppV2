package com.garepas.garepasapp.domain.model;

import com.garepas.garepasapp.domain.enums.FormaPago;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

  





@Entity
@Table(name = "abonos", indexes = {
        @Index(name = "idx_abonos_venta_id", columnList = "venta_id"),
        @Index(name = "idx_abonos_fecha", columnList = "fecha")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Abono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaPago metodo;

    @Column(length = 150)
    private String nota;
}
