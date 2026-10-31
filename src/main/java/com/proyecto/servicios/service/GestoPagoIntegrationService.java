package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxXmlResponse;

public interface GestoPagoIntegrationService {

    GestoPagoSendTxXmlResponse enviarTransaccion(GestoPagoSendTxPayload payload);
}

