package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

import org.springframework.http.HttpStatus;

public class RfcDuplicadoException extends ClienteException {
    public RfcDuplicadoException(String rfc) {
        super("CLIENTE-003", "El RFC proporcionado ya se encuentra registrado en el sistema", HttpStatus.CONFLICT);
    }
}
