package com.proyecto.servicios.model.pago;

import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculoOperacionPago {

    private GestoPagoSendTxPayload payload;
    private BigDecimal monto;
    private BigDecimal comision;
    private BigDecimal totalCobrado;
    private String telefonoEnviado;
    private String referenciaEnviada;
}

