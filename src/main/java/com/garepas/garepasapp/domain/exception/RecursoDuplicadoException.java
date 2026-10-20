package com.garepas.garepasapp.domain.exception;

public class RecursoDuplicadoException extends RuntimeException {

    public RecursoDuplicadoException(String recurso, String campo, String valor) {
        super(recurso + " con " + campo + " '" + valor + "' ya existe");
    }
}
