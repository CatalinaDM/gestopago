package com.proyecto.servicios.model.catalogo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.enums.ModoCaptura;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductoFormularioResponse {

    private Integer idProducto;
    private String nombre;
    private BigDecimal precio;
    private ModoCaptura modoCaptura;
    private Boolean requiereTelefono;
    private Boolean requiereReferencia;
    private Boolean requiereMonto;
    private Boolean montoFijo;
    private String etiquetaCampo;
    private String leyenda;
}

