package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

public class GestoPagoAuthException extends BusinessException {

    public GestoPagoAuthException(String detalle) {
        super(ErrorCode.GESTOPAGO_AUTH_ERROR, detalle);
    }
}

