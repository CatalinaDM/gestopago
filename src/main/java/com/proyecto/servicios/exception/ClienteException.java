package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ClienteException extends RuntimeException {

    private final String codigo;
    private final HttpStatus estado;

    public ClienteException(String codigo, String mensaje, HttpStatus estado) {
        super(mensaje);
        this.codigo = codigo;
        this.estado = estado;
    }
}
