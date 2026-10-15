package com.proyecto.servicios.model.cuenta;

import com.proyecto.servicios.model.GenericResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SaldoCuentaResponse extends GenericResponse {

    private String numeroCuenta;
    private BigDecimal saldo;
    private Boolean activo;
}
