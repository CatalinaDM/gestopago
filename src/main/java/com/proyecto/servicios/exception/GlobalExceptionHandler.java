package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CatalogoException.class)
    public ResponseEntity<ApiErrorResponse> manejarCatalogo(CatalogoException exception) {
        return ResponseEntity.status(exception.getEstado())
                .body(new ApiErrorResponse(exception.getCodigo(), exception.getMessage()));
    }
}