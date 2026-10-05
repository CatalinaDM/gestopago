package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CredencialesInvalidasException extends ClienteException {
    public CredencialesInvalidasException() {
        super("AUTH-003", "Credenciales de acceso inválidas", HttpStatus.UNAUTHORIZED);
    }
}
