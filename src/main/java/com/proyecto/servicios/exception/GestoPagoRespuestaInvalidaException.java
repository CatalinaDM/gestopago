package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

public class GestoPagoRespuestaInvalidaException extends BusinessException {

    public GestoPagoRespuestaInvalidaException(String detalle) {
        super(ErrorCode.GESTOPAGO_RESPUESTA_INVALIDA, detalle);
    }
}

