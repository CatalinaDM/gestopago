package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class AccesoDenegadoException extends ClienteException {

    public AccesoDenegadoException(String mensaje) {
        super("AUTH-004", mensaje, HttpStatus.FORBIDDEN);
    }

    public AccesoDenegadoException(String codigo, String mensaje) {
        super(codigo, mensaje, HttpStatus.FORBIDDEN);
    }
}
