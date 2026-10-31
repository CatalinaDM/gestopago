package com.proyecto.servicios.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    SUCCESS(0, "Operación exitosa", HttpStatus.OK),

    // Validaciones de negocio
    VALIDACION_ERROR(1, "Error de validación en los datos de entrada", HttpStatus.BAD_REQUEST),
    MENOR_DE_EDAD(2, "El cliente debe ser mayor de edad (18 años o más)", HttpStatus.BAD_REQUEST),
    CONTRASENA_INVALIDA(3, "La contraseña actual no es correcta", HttpStatus.BAD_REQUEST),
    CONTRASENA_IGUAL(4, "La nueva contraseña no puede ser igual a la anterior", HttpStatus.BAD_REQUEST),

    // Conflictos / Duplicados
    CURP_DUPLICADA(10, "Ya existe un registrado con la CURP indicada", HttpStatus.CONFLICT),
    RFC_DUPLICADO(11, "Ya existe un registrado con el RFC indicado", HttpStatus.CONFLICT),
    CORREO_DUPLICADO(12, "Ya existe un registro con el correo electrónico indicado", HttpStatus.CONFLICT),

    // No encontrados
    CLIENTE_NO_ENCONTRADO(20, "Cliente no encontrado", HttpStatus.NOT_FOUND),
    USUARIO_NO_ENCONTRADO(21, "Usuario no encontrado", HttpStatus.NOT_FOUND),
    CUENTA_NO_ENCONTRADA(22, "Cuenta no encontrada", HttpStatus.NOT_FOUND),

    // Autenticación y Autorización
    CREDENCIALES_INVALIDAS(30, "Credenciales de acceso incorrectas", HttpStatus.UNAUTHORIZED),
    USUARIO_INACTIVO(31, "El usuario se encuentra inactivo o bloqueado", HttpStatus.FORBIDDEN),
    ACCESO_DENEGADO(32, "No tiene permisos suficientes para realizar esta operación", HttpStatus.FORBIDDEN),
    NIP_INVALIDO(33, "El NIP de autorización transaccional es incorrecto", HttpStatus.UNAUTHORIZED),
    NIP_BLOQUEADO(34, "El NIP transaccional se encuentra bloqueado temporalmente por exceder intentos fallidos", HttpStatus.FORBIDDEN),
    NIP_NO_CONFIGURADO(35, "El usuario no cuenta con un NIP transaccional configurado", HttpStatus.FORBIDDEN),

    // Pagos y Cuentas
    PRODUCTO_NO_ENCONTRADO(40, "El producto solicitado no existe en el catálogo", HttpStatus.NOT_FOUND),
    PRODUCTO_NO_SOPORTADO(41, "El producto seleccionado no está soportado para pago", HttpStatus.BAD_REQUEST),
    CUENTA_INACTIVA(42, "La cuenta bancaria se encuentra inactiva", HttpStatus.CONFLICT),
    SALDO_INSUFICIENTE(43, "Saldo insuficiente para realizar la operación", HttpStatus.CONFLICT),
    CLIENTE_INACTIVO(44, "El cliente asociado se encuentra inactivo", HttpStatus.FORBIDDEN),

    // Integración GestoPago
    GESTOPAGO_RECHAZO_PROVEEDOR(50, "El proveedor de servicios rechazó la transacción", HttpStatus.BAD_REQUEST),
    GESTOPAGO_AUTH_ERROR(51, "Error de autenticación con el proveedor de servicios", HttpStatus.BAD_GATEWAY),
    GESTOPAGO_RESPUESTA_INVALIDA(52, "Respuesta inválida del proveedor de servicios", HttpStatus.BAD_GATEWAY),
    GESTOPAGO_COMUNICACION_ERROR(53, "Error de comunicación con el proveedor de servicios", HttpStatus.BAD_GATEWAY),
    GESTOPAGO_TIMEOUT_EN_REVISION(54, "La operación excedió el tiempo de respuesta y quedó en revisión", HttpStatus.CONFLICT),
    GESTOPAGO_CIFRADO_ERROR(55, "Error al procesar el cifrado de seguridad de la transacción", HttpStatus.INTERNAL_SERVER_ERROR),

    // Errores de integración o servidor
    ERROR_INTERNO(99, "Ha ocurrido un error inesperado en el servidor", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR_BASE_DATOS(100, "Error de persistencia en la base de datos", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
