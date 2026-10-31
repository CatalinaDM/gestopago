package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"idProducto", "idServicio", "telefono", "horaLocal", "referencia", "montoPago", "upc", "unidad"})
public class GestoPagoSendTxPayload {

    private Integer idProducto;
    private Integer idServicio;

    @JsonIgnore
    private Integer tipoFront;

    private String telefono;
    private String horaLocal;
    private String referencia;

    @JsonRawValue
    private String montoPago;

    private String upc;
    private String unidad;
}


