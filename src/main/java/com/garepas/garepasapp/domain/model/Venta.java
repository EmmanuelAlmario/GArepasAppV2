package com.garepas.garepasapp.domain.model;

import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.enums.FormaPago;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Venta registrada en el punto de venta.
 *
 * <p>La jornada es una relacion real (antes era un {@code jornadaId} suelto sin
 * clave foranea: al borrar una jornada las ventas quedaban apuntando a la nada).
 *
 * <p><b>Pagos:</b> una venta tiene forma de pago. Si es a CREDITO queda con
 * saldo pendiente y se cobra con abonos (ver {@link Abono}). El total es
 * {@code subtotal + impuestos}: el impuesto sale de la configuracion del
 * negocio.
 */
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

    /** Suma de los detalles, sin impuestos. */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Impuestos calculados al momento de la venta (configuracion del negocio). */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal impuestos = BigDecimal.ZERO;

    /** Total cobrado o por cobrar: subtotal + impuestos. */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    /** Saldo pendiente de cobro. 0 en ventas pagadas. */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldo = BigDecimal.ZERO;

    /** Forma de pago de la venta. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FormaPago formaPago = FormaPago.EFECTIVO;

    /** Fecha limite de pago de una venta a credito. */
    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    /** Cliente al que se le vendio (null = venta de mostrador). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Nombre del cliente tal como quedo en el momento de la venta (copia). */
    @Column(name = "nombre_cliente", length = 100)
    private String nombreCliente;

    /** Jornada de operacion a la que pertenece esta venta (null = fuera de jornada). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jornada_id")
    private Jornada jornada;

    /** ACTIVA o ANULADA. Las anuladas se conservan como historia pero no cuentan. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoVenta estado = EstadoVenta.ACTIVA;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleVenta> detalles = new ArrayList<>();

    /** Abonos recibidos (solo en ventas a credito). */
    @OneToMany(mappedBy = "venta")
    @Builder.Default
    private List<Abono> abonos = new ArrayList<>();

    public boolean estaActiva() {
        return estado == null || estado.activa();
    }

    /** true si la venta quedo pendiente de cobro. */
    public boolean estaPendiente() {
        return saldo != null && saldo.signum() > 0;
    }
}
