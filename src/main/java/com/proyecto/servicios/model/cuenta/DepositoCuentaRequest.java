package com.proyecto.servicios.model.cuenta;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositoCuentaRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El número de cuenta debe contener exactamente 10 dígitos")
    private String numeroCuenta;

    @NotNull(message = "El monto a depositar es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto a depositar debe ser mayor a cero")
    private BigDecimal monto;
}

