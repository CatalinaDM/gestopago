package com.proyecto.servicios.service.pago.strategy;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.enums.ModoCaptura;
import com.proyecto.servicios.model.pago.CalculoOperacionPago;
import com.proyecto.servicios.model.pago.RealizarPagoRequest;

public interface PagoPayloadStrategy {

    ModoCaptura obtenerModoCaptura();

    CalculoOperacionPago construirYValidar(
            GestoPagoProducto productoCatalogo,
            RealizarPagoRequest request,
            String upc,
            String horaLocal,
            String unidad
    );
}

