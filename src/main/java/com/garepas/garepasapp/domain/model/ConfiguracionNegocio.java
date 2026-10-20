package com.garepas.garepasapp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuracion del negocio (fila unica).
 *
 * <p>Nombre comercial, datos fiscales, impuesto por defecto y mensaje del
 * recibo. Es lo que permite que el POS y el recibo se vean como los de un
 * negocio concreto y no como una app generica.
 */
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

    /** Identificacion fiscal (NIT / RUC / RIF segun el pais). */
    @Column(length = 40)
    private String identificacionFiscal;

    @Column(length = 30)
    private String telefono;

    @Column(length = 150)
    private String direccion;

    /**
     * Impuesto por defecto aplicado a las ventas, en porcentaje.
     * 0 = sin impuesto.
     */
    @Column(name = "impuesto_porcentaje", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal impuestoPorcentaje = BigDecimal.ZERO;

    /** Mensaje que aparece al pie del recibo (gracias, redes, etc.). */
    @Column(name = "mensaje_recibo", length = 250)
    private String mensajeRecibo;

    /** Ruta del logo dentro de /images o URL externa. */
    @Column(name = "logo_url", length = 250)
    private String logoUrl;

    @Column(name = "actualizado_en", nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
