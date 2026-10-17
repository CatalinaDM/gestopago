package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

import org.springframework.http.HttpStatus;

public class CuentaNoEncontradaException extends ClienteException {
    public CuentaNoEncontradaException(String numeroCuenta) {
        super(ErrorCode.CUENTA_NO_ENCONTRADA, "La cuenta bancaria solicitada no existe o no fue encontrada");
    }
}
