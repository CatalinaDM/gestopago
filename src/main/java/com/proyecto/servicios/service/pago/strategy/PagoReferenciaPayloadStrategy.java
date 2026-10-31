package com.proyecto.servicios.service.pago.strategy;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.enums.ModoCaptura;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import com.proyecto.servicios.model.pago.CalculoOperacionPago;
import com.proyecto.servicios.model.pago.RealizarPagoRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

@Component
public class PagoReferenciaPayloadStrategy implements PagoPayloadStrategy {

    public static final String TELEFONO_FIJO_SERVICIOS = "1111111111";
    private static final Pattern REFERENCIA_ALFANUMERICA = Pattern.compile("^[a-zA-Z0-9]{1,100}$");

    @Override
    public ModoCaptura obtenerModoCaptura() {
        return ModoCaptura.PAGO_REFERENCIA;
    }

    @Override
    public CalculoOperacionPago construirYValidar(
            GestoPagoProducto productoCatalogo,
            RealizarPagoRequest request,
            String upc,
            String horaLocal,
            String unidad) {

        String referencia = request.getReferencia() != null ? request.getReferencia().trim() : "";
        if (!REFERENCIA_ALFANUMERICA.matcher(referencia).matches()) {
            throw new ValidacionException("PAGO-VAL-003",
                    "La referencia de pago (tipoReferencia 'b') es obligatoria y debe ser alfanumérica de 1 a 100 caracteres");
        }

        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException("PAGO-VAL-004",
                    "El monto a pagar es obligatorio y debe ser mayor a cero para pago de servicios");
        }

        BigDecimal montoPago = request.getMonto().setScale(2, RoundingMode.HALF_UP);
        BigDecimal comision = parsearComision(productoCatalogo.getPrecio());
        BigDecimal totalCobrado = montoPago.add(comision).setScale(2, RoundingMode.HALF_UP);
        String montoPagoJson = montoPago.stripTrailingZeros().toPlainString();

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idServicio(productoCatalogo.getIdServicio())
                .idProducto(productoCatalogo.getIdProducto())
                .tipoFront(productoCatalogo.getTipoFront() != null ? productoCatalogo.getTipoFront() : 2)
                .telefono(TELEFONO_FIJO_SERVICIOS)
                .referencia(referencia)
                .horaLocal(horaLocal)
                .montoPago(montoPagoJson)
                .upc(upc)
                .unidad(unidad)
                .build();

        return CalculoOperacionPago.builder()
                .payload(payload)
                .monto(montoPago)
                .comision(comision)
                .totalCobrado(totalCobrado)
                .telefonoEnviado(TELEFONO_FIJO_SERVICIOS)
                .referenciaEnviada(referencia)
                .build();
    }

    private BigDecimal parsearComision(String precioStr) {
        if (precioStr == null || precioStr.isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        try {
            BigDecimal comision = new BigDecimal(precioStr.trim());
            return comision.compareTo(BigDecimal.ZERO) < 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : comision.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }
}

