package com.garepas.garepasapp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

  






@Entity
@Table(name = "configuracion_negocio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_comercial", nullable = false, length = 80)
    private String nombreComercial;

      
    @Column(length = 40)
    private String identificacionFiscal;

    @Column(length = 30)
    private String telefono;

    @Column(length = 150)
    private String direccion;

      



    @Column(name = "impuesto_porcentaje", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal impuestoPorcentaje = BigDecimal.ZERO;

      
    @Column(name = "mensaje_recibo", length = 250)
    private String mensajeRecibo;

      
    @Column(name = "logo_url", length = 250)
    private String logoUrl;

    @Column(name = "actualizado_en", nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
