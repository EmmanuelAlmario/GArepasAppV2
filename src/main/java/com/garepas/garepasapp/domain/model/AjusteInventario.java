package com.garepas.garepasapp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trazabilidad de los ajustes manuales de inventario.
 *
 * <p>Antes el ajuste de stock mutaba el insumo y nada mas: no quedaba
 * registro de quien lo hizo, por que ni cuanto valia. Este modelo es la
 * bitacora del inventario y respalda la auditoria del negocio.
 */
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

    /** Variacion aplicada al stock (positiva = entrada, negativa = salida). */
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal delta;

    /** Stock del insumo despues de aplicar el ajuste. */
    @Column(name = "stock_resultante", nullable = false, precision = 15, scale = 4)
    private BigDecimal stockResultante;

    /** Valor del movimiento en pesos (delta * precio por unidad base). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 150)
    private String motivo;

    /** Usuario que ejecuto el ajuste. */
    @Column(nullable = false, length = 60)
    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
