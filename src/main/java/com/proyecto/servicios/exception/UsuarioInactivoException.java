package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class UsuarioInactivoException extends ClienteException {

    public UsuarioInactivoException(String correo) {
        super("AUTH-002", "El usuario con correo '" + correo + "' se encuentra inactivo o bloqueado. Por favor, contacte al administrador.", HttpStatus.FORBIDDEN);
    }

    public UsuarioInactivoException(String codigo, String mensaje) {
        super(codigo, mensaje, HttpStatus.FORBIDDEN);
    }
}
