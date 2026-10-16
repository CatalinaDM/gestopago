package com.proyecto.servicios.model.cliente;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteAccionRequest {

    @NotNull(message = "El id del cliente es obligatorio")
    private Integer id;
}
