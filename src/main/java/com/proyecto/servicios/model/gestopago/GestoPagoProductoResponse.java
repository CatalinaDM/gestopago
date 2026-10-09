package com.proyecto.servicios.model.gestopago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GestoPagoProductoResponse {

    private Integer id;
    private Integer idServicio;
    private Integer idProducto;
    private Integer idCatTipoServicio;
    private String producto;
    private String servicio;
    private Integer tipoFront;
    private Boolean hasDigitoVerificador;
    private String tipoReferencia;
    private String precio;
    private Boolean showAyuda;
    private String legend;
}
