package com.garepas.garepasapp.domain.model;

import com.garepas.garepasapp.domain.enums.FormaPago;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Abono: pago recibido de una venta a credito.
 *
 * <p>Cada abono baja el saldo de la venta. Cuando el saldo llega a cero, la
 * venta queda pagada. La bitacora completa de cobros queda en esta tabla.
 */
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
