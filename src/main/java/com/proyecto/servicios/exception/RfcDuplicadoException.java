package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class RfcDuplicadoException extends ClienteException {
    public RfcDuplicadoException(String rfc) {
        super("CLIENTE-003", "Ya existe un cliente registrado con el RFC: " + rfc, HttpStatus.CONFLICT);
    }
}
