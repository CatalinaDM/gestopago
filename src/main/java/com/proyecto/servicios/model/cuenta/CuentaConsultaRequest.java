package com.proyecto.servicios.model.cuenta;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaConsultaRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    private String numeroCuenta;
}
