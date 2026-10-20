package com.garepas.garepasapp.domain.model;

import com.garepas.garepasapp.domain.enums.UnidadMedida;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Insumo del inventario (materia prima).
 *
 * <p><b>Semantica de unidades:</b> {@code stockActual} y
 * {@code precioPorUnidadBase} estan siempre expresados en la unidad BASE
 * (gramo, mililitro o unidad). {@code unidadPresentacion} es la unidad que
 * eligio el usuario (kg, L, u) y sirve solo para mostrar y capturar.
 *
 * <p>Antes un unico campo {@code unidadMedida} pretendia ser las dos cosas a
 * la vez: el formulario guardaba la unidad base y el seeder guardaba la de
 * presentacion, por lo que el mismo dato significaba cosas distintas
 * depending de donde venia.
 */
@Entity
@Table(name = "insumos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_insumos_nombre", columnNames = "nombre")
}, indexes = {
        @Index(name = "idx_insumos_categoria", columnList = "categoria"),
        @Index(name = "idx_insumos_activo", columnList = "activo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String categoria;

    @Column(nullable = false, length = 30)
    private String marca;

    /** Stock disponible, expresado en la unidad BASE del insumo. */
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal stockActual;

    /** Umbral opcional para alerta de stock bajo (unidad base). null = sin alerta. */
    @Column(precision = 15, scale = 4)
    private BigDecimal stockMinimo;

    /**
     * Precio por unidad BASE (gramo, mililitro o unidad). Precision fina
     * porque puede ser una fraccion pequena ($0.0125/g).
     */
    @Column(name = "precio_unidad_base", nullable = false, precision = 15, scale = 6)
    private BigDecimal precioPorUnidadBase;

    /** Unidad de presentacion elegida por el usuario (kg, L, u, g, ml). */
    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_presentacion", nullable = false, length = 20)
    private UnidadMedida unidadPresentacion;

    @Column(nullable = false)
    private Boolean activo;

    @Version
    private Long version;

    /** Unidad base derivada: en ella estan stockActual y precioPorUnidadBase. */
    public UnidadMedida unidadBase() {
        return unidadPresentacion == null ? UnidadMedida.UNIDAD : unidadPresentacion.base();
    }

    /** Stock expresado en la unidad de presentacion (lo que ve el usuario). */
    public BigDecimal stockEnPresentacion() {
        return unidadPresentacion == null
                ? stockActual
                : unidadPresentacion.desdeBase(stockActual);
    }

    /** Stock minimo expresado en la unidad de presentacion. */
    public BigDecimal stockMinimoEnPresentacion() {
        return stockMinimo == null || unidadPresentacion == null
                ? stockMinimo
                : unidadPresentacion.desdeBase(stockMinimo);
    }

    /** Precio por unidad de presentacion (lo que paga el proveedor). */
    public BigDecimal precioEnPresentacion() {
        return precioPorUnidadBase == null || unidadPresentacion == null
                ? precioPorUnidadBase
                : unidadPresentacion.precioDesdeBase(precioPorUnidadBase);
    }

    /** true si el stock quedo por debajo del umbral configurado. */
    public boolean stockBajo() {
        return stockMinimo != null && stockActual.compareTo(stockMinimo) <= 0;
    }
}
