package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ClienteNoEncontradoException extends ClienteException {
    public ClienteNoEncontradoException(String criterio) {
        super("CLIENTE-005", "No se encontró el cliente con el criterio: " + criterio, HttpStatus.NOT_FOUND);
    }
}
