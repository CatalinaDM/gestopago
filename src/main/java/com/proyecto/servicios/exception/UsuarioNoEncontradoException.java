package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class UsuarioNoEncontradoException extends ClienteException {
    public UsuarioNoEncontradoException(String criterio) {
        super("AUTH-001", "No se encontró el usuario con el criterio: " + criterio, HttpStatus.NOT_FOUND);
    }
}
