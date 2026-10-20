package com.garepas.garepasapp.domain.enums;

  







public enum EstadoVenta {

      
    ACTIVA,

      
    ANULADA;

    public boolean activa() {
        return this == ACTIVA;
    }
}
