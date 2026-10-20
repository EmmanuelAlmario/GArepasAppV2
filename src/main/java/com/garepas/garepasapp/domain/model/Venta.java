package com.garepas.garepasapp.domain.model;

import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.enums.FormaPago;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

  










@Entity
@Table(name = "ventas", indexes = {
        @Index(name = "idx_ventas_fecha", columnList = "fecha"),
        @Index(name = "idx_ventas_jornada_id", columnList = "jornada_id"),
        @Index(name = "idx_ventas_estado", columnList = "estado"),
        @Index(name = "idx_ventas_cliente_id", columnList = "cliente_id"),
        @Index(name = "idx_ventas_saldo", columnList = "saldo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

      
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

      
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal impuestos = BigDecimal.ZERO;

      
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

      
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldo = BigDecimal.ZERO;

      
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FormaPago formaPago = FormaPago.EFECTIVO;

      
    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

      
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

      
    @Column(name = "nombre_cliente", length = 100)
    private String nombreCliente;

      
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jornada_id")
    private Jornada jornada;

      
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoVenta estado = EstadoVenta.ACTIVA;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleVenta> detalles = new ArrayList<>();

      
    @OneToMany(mappedBy = "venta")
    @Builder.Default
    private List<Abono> abonos = new ArrayList<>();

    public boolean estaActiva() {
        return estado == null || estado.activa();
    }

      
    public boolean estaPendiente() {
        return saldo != null && saldo.signum() > 0;
    }
}
