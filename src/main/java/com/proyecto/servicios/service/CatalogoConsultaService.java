package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;

import java.util.List;

public interface CatalogoConsultaService {

    List<GestoPagoProducto> obtenerCatalogo();
}
