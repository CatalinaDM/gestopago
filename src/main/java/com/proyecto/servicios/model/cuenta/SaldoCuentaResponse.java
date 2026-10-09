package com.proyecto.servicios.model.cuenta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaldoCuentaResponse {

    private String numeroCuenta;
    private BigDecimal saldo;
    private Boolean activo;
}
