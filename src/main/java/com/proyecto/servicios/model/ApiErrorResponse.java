package com.proyecto.servicios.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiErrorResponse {

    private String codigo;
    private String mensaje;
}