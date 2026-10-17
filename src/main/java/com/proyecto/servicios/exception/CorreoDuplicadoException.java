package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

import org.springframework.http.HttpStatus;

public class CorreoDuplicadoException extends ClienteException {
    public CorreoDuplicadoException(String correo) {
        super("CLIENTE-004", "El correo electrónico proporcionado ya se encuentra registrado en el sistema", HttpStatus.CONFLICT);
    }
}
