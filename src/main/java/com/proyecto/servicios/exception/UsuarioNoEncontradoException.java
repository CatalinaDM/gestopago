package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

import org.springframework.http.HttpStatus;

public class UsuarioNoEncontradoException extends ClienteException {
    public UsuarioNoEncontradoException(String criterio) {
        super(ErrorCode.USUARIO_NO_ENCONTRADO, "El usuario solicitado no existe o no fue encontrado");
    }
}
