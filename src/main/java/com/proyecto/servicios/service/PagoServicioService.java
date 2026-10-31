package com.proyecto.servicios.service;

import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.pago.AutorizacionNipResponse;
import com.proyecto.servicios.model.pago.ComprobantePagoResponse;
import com.proyecto.servicios.model.pago.RealizarPagoRequest;
import com.proyecto.servicios.model.pago.SolicitarNipTransaccionRequest;

public interface PagoServicioService {

    ComprobantePagoResponse realizarPago(
            GenericRequest<RealizarPagoRequest> request,
            Integer usuarioId,
            Integer clienteId
    );

    AutorizacionNipResponse solicitarNipParaPago(
            GenericRequest<SolicitarNipTransaccionRequest> request,
            Integer usuarioId,
            Integer clienteId
    );
}

