package com.garepas.garepasapp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recetas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_recetas_nombre", columnNames = "nombre")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(length = 150)
    private String descripcion;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleReceta> detalles = new ArrayList<>();

      




    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_producido_id")
    private Insumo insumoProducido;

      
    @Column(precision = 15, scale = 4)
    private BigDecimal rendimiento;
}
