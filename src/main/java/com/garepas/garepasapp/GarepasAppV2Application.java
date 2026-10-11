package com.garepas.garepasapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class GarepasAppV2Application {

    /**
     * Fija la zona horaria del JVM (America/Bogota, UTC-5) ANTES de arrancar.
     * Sin esto, el contenedor usa UTC y toda fecha registrada por
     * LocalDateTime.now() queda con un desfase de +5 horas.
     */
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
    }

    public static void main(String[] args) {
        SpringApplication.run(GarepasAppV2Application.class, args);
    }
}