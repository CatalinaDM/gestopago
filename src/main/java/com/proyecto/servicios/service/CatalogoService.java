package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;

import java.util.List;
import java.util.Optional;

public interface CatalogoService {

    void actualizarCatalogo();

    Optional<List<GestoPagoProducto>> consultarCatalogoExterno();

}