package com.garepas.garepasapp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "jornadas", indexes = {
        @Index(name = "idx_jornadas_fecha_apertura", columnList = "fechaApertura")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Jornada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    @Column(nullable = false, length = 60)
    private String abiertaPor;

    private LocalDateTime fechaCierre;

    @Column(nullable = false)
    private Boolean activa;
}