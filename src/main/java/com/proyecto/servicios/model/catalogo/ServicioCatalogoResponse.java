package com.proyecto.servicios.model.catalogo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioCatalogoResponse {

    private Integer idServicio;
    private String servicio;
    private Integer idCatTipoServicio;
}

