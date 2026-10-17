package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detalle;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode != null ? errorCode.getMessage() : ErrorCode.ERROR_INTERNO.getMessage());
        this.errorCode = errorCode != null ? errorCode : ErrorCode.ERROR_INTERNO;
        this.detalle = null;
    }

    public BusinessException(ErrorCode errorCode, String detalle) {
        super(detalle != null ? detalle : (errorCode != null ? errorCode.getMessage() : ErrorCode.ERROR_INTERNO.getMessage()));
        this.errorCode = errorCode != null ? errorCode : ErrorCode.ERROR_INTERNO;
        this.detalle = detalle;
    }
}
