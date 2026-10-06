package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CatalogoException.class)
    public ResponseEntity<ApiErrorResponse> manejarCatalogo(CatalogoException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getEstado())
                .body(new ApiErrorResponse(exception.getCodigo(), exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ClienteException.class)
    public ResponseEntity<ApiErrorResponse> manejarClienteException(ClienteException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getEstado())
                .body(new ApiErrorResponse(exception.getCodigo(), exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> manejarValidaciones(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("VALIDACION-001", mensaje, request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> manejarJsonMalformado(HttpMessageNotReadableException exception, HttpServletRequest request) {
        log.warn("HTTP-001: Petición con cuerpo JSON malformado o ilegible: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("HTTP-001", "El cuerpo de la petición contiene un formato JSON inválido", request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        log.warn("HTTP-002: Método HTTP no soportado: {}", exception.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiErrorResponse("HTTP-002", "Método HTTP no permitido: " + exception.getMethod(), request.getRequestURI()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> manejarBaseDeDatos(DataAccessException exception, HttpServletRequest request) {
        log.error("DB-001: Error de persistencia en base de datos: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("DB-001", "Ocurrió un error al procesar la operación en base de datos", request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> manejarIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("ARGUMENTO-001", exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> manejarInesperado(Exception exception, HttpServletRequest request) {
        log.error("SYS-999: Error no controlado en la aplicación ({})", exception.getClass().getSimpleName(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("SYS-999", "Ha ocurrido un error inesperado en el servidor", request.getRequestURI()));
    }
}