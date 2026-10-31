package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;

public class ProductoNoSoportadoException extends ClienteException {

    public ProductoNoSoportadoException(Integer idProducto) {
        super(ErrorCode.PRODUCTO_NO_SOPORTADO, "El producto con ID " + idProducto + " no está soportado para pago");
    }

    public ProductoNoSoportadoException(String detalle) {
        super(ErrorCode.PRODUCTO_NO_SOPORTADO, detalle);
    }
}

