package com.proyecto.servicios.model.pago;

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
public class RealizarPagoRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El número de cuenta debe contener exactamente 10 dígitos")
    private String numeroCuenta;

    @NotNull(message = "El idProducto es obligatorio")
    private Integer idProducto;

    private String telefono;

    private String referencia;

    private BigDecimal monto;

    @NotBlank(message = "El NIP de autorización es obligatorio")
    private String pin;
}

