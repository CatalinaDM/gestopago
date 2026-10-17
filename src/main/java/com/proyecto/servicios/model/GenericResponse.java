package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse {

    private Integer codigo;
    private String mensaje;

    public GenericResponse(Integer codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
    }

    public static GenericResponse success(String mensaje) {
        return new GenericResponse(0, mensaje);
    }

    public static GenericResponse error(Integer codigo, String mensaje) {
        return new GenericResponse(codigo, mensaje);
    }
}