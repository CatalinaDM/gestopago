package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ContrasenaInvalidaException extends ClienteException {
    public ContrasenaInvalidaException(String mensaje) {
        super("AUTH-004", mensaje, HttpStatus.BAD_REQUEST);
    }
}
