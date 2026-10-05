package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CorreoDuplicadoException extends ClienteException {
    public CorreoDuplicadoException(String correo) {
        super("CLIENTE-004", "Ya existe un cliente o usuario registrado con el correo: " + correo, HttpStatus.CONFLICT);
    }
}
