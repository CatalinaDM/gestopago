package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

import org.springframework.http.HttpStatus;

public class CurpDuplicadaException extends ClienteException {
    public CurpDuplicadaException(String curp) {
        super("CLIENTE-002", "La CURP proporcionada ya se encuentra registrada en el sistema", HttpStatus.CONFLICT);
    }
}
