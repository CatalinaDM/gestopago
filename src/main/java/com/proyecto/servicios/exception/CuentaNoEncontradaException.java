package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CuentaNoEncontradaException extends ClienteException {
    public CuentaNoEncontradaException(String numeroCuenta) {
        super("CUENTA-001", "No se encontró la cuenta bancaria con el número: " + numeroCuenta, HttpStatus.NOT_FOUND);
    }
}
