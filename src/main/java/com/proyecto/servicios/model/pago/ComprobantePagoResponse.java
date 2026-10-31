package com.proyecto.servicios.model.pago;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.model.GenericResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComprobantePagoResponse extends GenericResponse {

    private Integer idTransaccion;
    private String upc;
    private String numeroCuenta;
    private Integer idServicio;
    private Integer idProducto;
    private String servicio;
    private String producto;
    private String telefono;
    private String referencia;
    private BigDecimal monto;
    private BigDecimal comision;
    private BigDecimal totalCobrado;
    private BigDecimal saldoDisponible;
    private EstadoTransaccion estado;
    private String idTxProveedor;
    private String numAutorizacion;
    private String pin;
    private String folio;
    private String leyenda;
    private LocalDateTime fechaOperacion;
}

