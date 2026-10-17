package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

import org.springframework.http.HttpStatus;

public class ClienteNoEncontradoException extends ClienteException {
    public ClienteNoEncontradoException(String criterio) {
        super(ErrorCode.CLIENTE_NO_ENCONTRADO, "El cliente solicitado no existe o no fue encontrado");
    }
}
