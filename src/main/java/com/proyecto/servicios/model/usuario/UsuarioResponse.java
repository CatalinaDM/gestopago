package com.proyecto.servicios.model.usuario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private Integer id;
    private Integer clienteId;
    private String correo;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
