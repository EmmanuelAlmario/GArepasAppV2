package com.garepas.garepasapp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas", indexes = {
        @Index(name = "idx_ventas_fecha", columnList = "fecha")
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
    private BigDecimal total;

    /** Nombre del cliente (texto libre, no se liga a deudas ni cuentas). */
    @Column(name = "nombre_cliente", length = 100)
    private String nombreCliente;

    /** Jornada de operación a la que pertenece esta venta (null = fuera de jornada). */
    @Column(name = "jornada_id")
    private Long jornadaId;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<DetalleVenta> detalles = new java.util.ArrayList<>();
}
