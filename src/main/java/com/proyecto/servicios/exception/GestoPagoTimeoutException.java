package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

public class GestoPagoTimeoutException extends BusinessException {

    public GestoPagoTimeoutException(String detalle) {
        super(ErrorCode.GESTOPAGO_TIMEOUT_EN_REVISION, detalle);
    }
}

