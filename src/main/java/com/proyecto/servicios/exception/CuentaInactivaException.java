package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

public class CuentaInactivaException extends ClienteException {

    public CuentaInactivaException() {
        super(ErrorCode.CUENTA_INACTIVA);
    }

    public CuentaInactivaException(String numeroCuenta) {
        super(ErrorCode.CUENTA_INACTIVA, "La cuenta bancaria " + numeroCuenta + " se encuentra inactiva");
    }
}

