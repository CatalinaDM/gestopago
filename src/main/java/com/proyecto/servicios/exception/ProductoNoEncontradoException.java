package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

public class ProductoNoEncontradoException extends ClienteException {

    public ProductoNoEncontradoException(Integer idProducto) {
        super(ErrorCode.PRODUCTO_NO_ENCONTRADO, "No se encontró el producto con ID: " + idProducto);
    }

    public ProductoNoEncontradoException(String detalle) {
        super(ErrorCode.PRODUCTO_NO_ENCONTRADO, detalle);
    }
}

