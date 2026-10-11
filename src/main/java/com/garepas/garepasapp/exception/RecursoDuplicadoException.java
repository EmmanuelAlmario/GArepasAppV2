package com.garepas.garepasapp.exception;

public class RecursoDuplicadoException extends RuntimeException {

    public RecursoDuplicadoException(String recurso, String campo, String valor) {
        super(recurso + " con " + campo + " '" + valor + "' ya existe");
    }
}
