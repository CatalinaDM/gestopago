package com.proyecto.servicios.model.usuario;

import com.proyecto.servicios.model.GenericResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UsuarioResponse extends GenericResponse {

    private Integer id;
    private Integer clienteId;
    private String correo;
    private Integer rol;
    private Integer intentosFallidos;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
