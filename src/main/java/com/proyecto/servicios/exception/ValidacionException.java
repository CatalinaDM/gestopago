package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ValidacionException extends ClienteException {
    public ValidacionException(String mensaje) {
        super("VALIDACION-001", mensaje, HttpStatus.BAD_REQUEST);
    }

    public ValidacionException(String codigo, String mensaje) {
        super(codigo, mensaje, HttpStatus.BAD_REQUEST);
    }
}
