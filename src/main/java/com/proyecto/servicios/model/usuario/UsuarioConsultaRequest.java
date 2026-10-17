package com.proyecto.servicios.model.usuario;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioConsultaRequest {

    @NotNull(message = "El id del usuario es obligatorio")
    private Integer id;
}
