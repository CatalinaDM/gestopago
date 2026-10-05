package com.proyecto.servicios.model.cuenta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaResponse {

    private Integer id;
    private Integer clienteId;
    private String numeroCuenta;
    private BigDecimal saldo;
    private String estatus;
    private LocalDateTime fechaCreacion;
}
