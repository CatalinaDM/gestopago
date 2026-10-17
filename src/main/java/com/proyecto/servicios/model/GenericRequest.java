package com.proyecto.servicios.model;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clase wrapper genérica para estandarizar el cuerpo de todas las peticiones.
 * El campo "datos" contiene el DTO específico de cada operación.
 *
 * @param <T> Tipo del DTO de datos que se envía en la petición
 */
@Getter
@Setter
@NoArgsConstructor
public class GenericRequest<T> {

    @Valid
    private T datos;
}
