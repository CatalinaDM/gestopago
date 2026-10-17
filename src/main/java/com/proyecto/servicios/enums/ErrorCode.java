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
    CURP_DUPLICADA(10, "Ya existe un cliente registrado con la CURP indicada", HttpStatus.CONFLICT),
    RFC_DUPLICADO(11, "Ya existe un cliente registrado con el RFC indicado", HttpStatus.CONFLICT),
    CORREO_DUPLICADO(12, "Ya existe un cliente registrado con el correo electrónico indicado", HttpStatus.CONFLICT),

    // No encontrados
    CLIENTE_NO_ENCONTRADO(20, "Cliente no encontrado", HttpStatus.NOT_FOUND),
    USUARIO_NO_ENCONTRADO(21, "Usuario no encontrado", HttpStatus.NOT_FOUND),
    CUENTA_NO_ENCONTRADA(22, "Cuenta no encontrada", HttpStatus.NOT_FOUND),

    // Autenticación y Autorización
    CREDENCIALES_INVALIDAS(30, "Credenciales de acceso incorrectas", HttpStatus.UNAUTHORIZED),
    USUARIO_INACTIVO(31, "El usuario se encuentra inactivo o bloqueado", HttpStatus.FORBIDDEN),
    ACCESO_DENEGADO(32, "No tiene permisos suficientes para realizar esta operación", HttpStatus.FORBIDDEN),

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
