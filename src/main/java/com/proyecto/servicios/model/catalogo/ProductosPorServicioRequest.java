package com.proyecto.servicios.model.catalogo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductosPorServicioRequest {

    @NotNull(message = "El idServicio es obligatorio")
    private Integer idServicio;
}

