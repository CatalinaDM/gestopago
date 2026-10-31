package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

public class SaldoInsuficienteException extends ClienteException {

    public SaldoInsuficienteException() {
        super(ErrorCode.SALDO_INSUFICIENTE);
    }

    public SaldoInsuficienteException(String detalle) {
        super(ErrorCode.SALDO_INSUFICIENTE, detalle);
    }
}

