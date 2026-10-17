package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.ErrorCode;
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

    /**
     * Manejador centralizado y unificado para todas las excepciones de negocio.
     * Utiliza el ErrorCode enum para determinar el código, mensaje y status HTTP.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> manejarBusinessException(BusinessException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode() != null ? exception.getErrorCode() : ErrorCode.ERROR_INTERNO;
        HttpStatus status = errorCode.getHttpStatus() != null ? errorCode.getHttpStatus() : HttpStatus.BAD_REQUEST;
        String mensaje = exception.getMessage() != null ? exception.getMessage() : errorCode.getMessage();

        log.warn("Negocio [{}]: {}", errorCode.name(), mensaje);
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(String.valueOf(errorCode.getCode()), mensaje, request.getRequestURI()));
    }

    /**
     * Errores de validación de Bean Validation (@Valid, @NotNull, @NotBlank, @Size, @Pattern)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> manejarValidaciones(MethodArgumentNotValidException exception, HttpServletRequest request) {
        java.util.Map<String, String> erroresDetallados = new java.util.LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            erroresDetallados.put(error.getField(), error.getDefaultMessage());
        }

        String mensajeResumen = exception.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        String.valueOf(ErrorCode.VALIDACION_ERROR.getCode()),
                        mensajeResumen,
                        erroresDetallados,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> manejarJsonMalformado(HttpMessageNotReadableException exception, HttpServletRequest request) {
        log.warn("JSON malformado: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        String.valueOf(ErrorCode.VALIDACION_ERROR.getCode()),
                        "El cuerpo de la petición contiene un formato JSON inválido o ilegible",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        log.warn("Método HTTP no soportado: {}", exception.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiErrorResponse("HTTP-002", "Método HTTP no permitido: " + exception.getMethod(), request.getRequestURI()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> manejarBaseDeDatos(DataAccessException exception, HttpServletRequest request) {
        log.error("Error persistencia BD: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        String.valueOf(ErrorCode.ERROR_BASE_DATOS.getCode()),
                        ErrorCode.ERROR_BASE_DATOS.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> manejarInesperado(Exception exception, HttpServletRequest request) {
        log.error("Error no controlado ({})", exception.getClass().getSimpleName(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        String.valueOf(ErrorCode.ERROR_INTERNO.getCode()),
                        ErrorCode.ERROR_INTERNO.getMessage(),
                        request.getRequestURI()
                ));
    }
}