package com.garepas.garepasapp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

  






@Entity
@Table(name = "clientes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_clientes_nombre", columnNames = "nombre")
}, indexes = {
        @Index(name = "idx_clientes_activo", columnList = "activo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 30)
    private String telefono;

    @Column(length = 150)
    private String direccion;

    @Column(length = 250)
    private String notas;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = Boolean.TRUE;

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
