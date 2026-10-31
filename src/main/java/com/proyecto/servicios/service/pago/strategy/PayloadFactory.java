package com.proyecto.servicios.service.pago.strategy;

import com.proyecto.servicios.enums.ModoCaptura;
import com.proyecto.servicios.exception.ProductoNoSoportadoException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PayloadFactory {

    private final Map<ModoCaptura, PagoPayloadStrategy> estrategiasPorModo;

    public PayloadFactory(List<PagoPayloadStrategy> estrategias) {
        this.estrategiasPorModo = new EnumMap<>(ModoCaptura.class);
        if (estrategias != null) {
            for (PagoPayloadStrategy estrategia : estrategias) {
                this.estrategiasPorModo.put(estrategia.obtenerModoCaptura(), estrategia);
            }
        }
    }

    public PagoPayloadStrategy obtenerEstrategia(ModoCaptura modoCaptura) {
        if (modoCaptura == null) {
            throw new ProductoNoSoportadoException("El modo de captura del producto no está definido");
        }
        PagoPayloadStrategy estrategia = estrategiasPorModo.get(modoCaptura);
        if (estrategia == null) {
            throw new ProductoNoSoportadoException("El modo de captura " + modoCaptura + " no está soportado para pago");
        }
        return estrategia;
    }
}

