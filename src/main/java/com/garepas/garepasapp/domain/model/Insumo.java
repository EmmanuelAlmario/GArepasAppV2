package com.garepas.garepasapp.domain.model;

import com.garepas.garepasapp.domain.enums.UnidadMedida;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

  












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

      
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal stockActual;

      
    @Column(precision = 15, scale = 4)
    private BigDecimal stockMinimo;

      



    @Column(name = "precio_unidad_base", nullable = false, precision = 15, scale = 6)
    private BigDecimal precioPorUnidadBase;

      
    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_presentacion", nullable = false, length = 20)
    private UnidadMedida unidadPresentacion;

    @Column(nullable = false)
    private Boolean activo;

    @Version
    private Long version;

      
    public UnidadMedida unidadBase() {
        return unidadPresentacion == null ? UnidadMedida.UNIDAD : unidadPresentacion.base();
    }

      
    public BigDecimal stockEnPresentacion() {
        return unidadPresentacion == null
                ? stockActual
                : unidadPresentacion.desdeBase(stockActual);
    }

      
    public BigDecimal stockMinimoEnPresentacion() {
        return stockMinimo == null || unidadPresentacion == null
                ? stockMinimo
                : unidadPresentacion.desdeBase(stockMinimo);
    }

      
    public BigDecimal precioEnPresentacion() {
        return precioPorUnidadBase == null || unidadPresentacion == null
                ? precioPorUnidadBase
                : unidadPresentacion.precioDesdeBase(precioPorUnidadBase);
    }

      
    public boolean stockBajo() {
        return stockMinimo != null && stockActual.compareTo(stockMinimo) <= 0;
    }
}
