package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

public class NipInvalidoException extends ClienteException {

    public NipInvalidoException() {
        super(ErrorCode.NIP_INVALIDO);
    }

    public NipInvalidoException(String detalle) {
        super(ErrorCode.NIP_INVALIDO, detalle);
    }

    public NipInvalidoException(ErrorCode errorCode, String detalle) {
        super(errorCode, detalle);
    }
}

