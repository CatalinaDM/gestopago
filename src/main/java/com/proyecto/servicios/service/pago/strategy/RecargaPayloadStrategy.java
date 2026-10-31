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
public class RecargaPayloadStrategy implements PagoPayloadStrategy {

    private static final Pattern TELEFONO_10_DIGITOS = Pattern.compile("^\\d{10}$");

    @Override
    public ModoCaptura obtenerModoCaptura() {
        return ModoCaptura.RECARGA;
    }

    @Override
    public CalculoOperacionPago construirYValidar(
            GestoPagoProducto productoCatalogo,
            RealizarPagoRequest request,
            String upc,
            String horaLocal,
            String unidad) {

        String telefono = request.getTelefono() != null ? request.getTelefono().trim() : "";
        if (!TELEFONO_10_DIGITOS.matcher(telefono).matches()) {
            throw new ValidacionException("PAGO-VAL-001",
                    "Para recargas (tipoReferencia 'a'), el teléfono es obligatorio y debe tener exactamente 10 dígitos numéricos");
        }

        BigDecimal precioCatalogo = parsearMontoPositivo(
                productoCatalogo.getPrecio(),
                "El precio del producto de recarga en el catálogo es inválido"
        ).setScale(2, RoundingMode.HALF_UP);

        if (request.getMonto() == null || request.getMonto().compareTo(precioCatalogo) != 0) {
            throw new ValidacionException("PAGO-VAL-006",
                    "El monto enviado ($" + (request.getMonto() != null ? request.getMonto() : "null")
                            + ") no coincide con el precio del producto en el catálogo ($" + precioCatalogo + ")");
        }

        BigDecimal comision = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idProducto(productoCatalogo.getIdProducto())
                .idServicio(productoCatalogo.getIdServicio())
                .tipoFront(productoCatalogo.getTipoFront() != null ? productoCatalogo.getTipoFront() : 1)
                .telefono(telefono)
                .horaLocal(horaLocal)
                .upc(upc)
                .unidad(unidad)
                .build();

        return CalculoOperacionPago.builder()
                .payload(payload)
                .monto(precioCatalogo)
                .comision(comision)
                .totalCobrado(precioCatalogo)
                .telefonoEnviado(telefono)
                .referenciaEnviada(null)
                .build();
    }

    private BigDecimal parsearMontoPositivo(String valor, String mensajeError) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacionException("PAGO-VAL-002", mensajeError);
        }
        try {
            BigDecimal bd = new BigDecimal(valor.trim());
            if (bd.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidacionException("PAGO-VAL-002", mensajeError);
            }
            return bd;
        } catch (NumberFormatException e) {
            throw new ValidacionException("PAGO-VAL-002", mensajeError);
        }
    }
}
