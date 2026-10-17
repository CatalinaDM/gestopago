package com.proyecto.servicios.model.cuenta;

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
public class CuentaResponse extends GenericResponse {

    private Integer id;
    private Integer clienteId;
    private String numeroCuenta;
    private BigDecimal saldo;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
