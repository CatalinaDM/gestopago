package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CatalogoException.class)
    public ResponseEntity<ApiErrorResponse> manejarCatalogo(CatalogoException exception) {
        return ResponseEntity.status(exception.getEstado())
                .body(new ApiErrorResponse(exception.getCodigo(), exception.getMessage()));
    }

    @ExceptionHandler(ClienteException.class)
    public ResponseEntity<ApiErrorResponse> manejarClienteException(ClienteException exception) {
        return ResponseEntity.status(exception.getEstado())
                .body(new ApiErrorResponse(exception.getCodigo(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> manejarValidaciones(MethodArgumentNotValidException exception) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("VALIDACION-001", mensaje));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> manejarIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("ARGUMENTO-001", exception.getMessage()));
    }
}