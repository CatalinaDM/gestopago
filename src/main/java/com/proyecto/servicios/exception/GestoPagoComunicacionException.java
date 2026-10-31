package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

public class GestoPagoComunicacionException extends BusinessException {

    public GestoPagoComunicacionException(String detalle) {
        super(ErrorCode.GESTOPAGO_COMUNICACION_ERROR, detalle);
    }
}

