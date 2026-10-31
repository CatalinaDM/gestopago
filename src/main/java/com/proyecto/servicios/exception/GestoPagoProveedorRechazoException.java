package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;
import lombok.Getter;

@Getter
public class GestoPagoProveedorRechazoException extends BusinessException {

    private final String codigoProveedor;

    public GestoPagoProveedorRechazoException(String codigoProveedor, String mensajeProveedor) {
        super(ErrorCode.GESTOPAGO_RECHAZO_PROVEEDOR,
                mensajeProveedor != null && !mensajeProveedor.isBlank()
                        ? "Rechazado por GestoPago [código " + codigoProveedor + "]: " + mensajeProveedor + " (Tu saldo fue reembolsado automáticamente)"
                        : ErrorCode.GESTOPAGO_RECHAZO_PROVEEDOR.getMessage());
        this.codigoProveedor = codigoProveedor;
    }
}

