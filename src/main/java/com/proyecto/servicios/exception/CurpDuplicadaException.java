package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CurpDuplicadaException extends ClienteException {
    public CurpDuplicadaException(String curp) {
        super("CLIENTE-002", "Ya existe un cliente registrado con la CURP: " + curp, HttpStatus.CONFLICT);
    }
}
