package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.List;

@Data
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoProductResponse {

    @XmlElement(name = "MENSAJE")
    private GestoPagoMensaje mensaje;

    @XmlElementWrapper(name = "PRODUCTOS")
    @XmlElement(name = "producto")
    private List<GestoPagoProducto> productos;
}