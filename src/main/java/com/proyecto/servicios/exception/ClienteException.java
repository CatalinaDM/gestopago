package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ClienteException extends BusinessException {

    private final String codigo;
    private final HttpStatus estado;

    public ClienteException(String codigo, String mensaje, HttpStatus estado) {
        super(ErrorCode.VALIDACION_ERROR, mensaje);
        this.codigo = codigo;
        this.estado = estado;
    }

    public ClienteException(ErrorCode errorCode) {
        super(errorCode);
        this.codigo = String.valueOf(errorCode.getCode());
        this.estado = errorCode.getHttpStatus();
    }

    public ClienteException(ErrorCode errorCode, String detalle) {
        super(errorCode, detalle);
        this.codigo = String.valueOf(errorCode.getCode());
        this.estado = errorCode.getHttpStatus();
    }
}
