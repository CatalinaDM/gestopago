package com.proyecto.servicios.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PersonaResponse extends GenericResponse {
    private String nombre;
    private String apellidoP;
    private String apellidoMaterno;
}
