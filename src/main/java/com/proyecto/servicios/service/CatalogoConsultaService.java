package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.catalogo.CategoriaCatalogoResponse;
import com.proyecto.servicios.model.catalogo.ProductoFormularioResponse;
import com.proyecto.servicios.model.catalogo.ServicioCatalogoResponse;

import java.util.List;
import java.util.Optional;

public interface CatalogoConsultaService {

    List<GestoPagoProducto> obtenerCatalogo();

    List<CategoriaCatalogoResponse> obtenerCategorias();

    List<ServicioCatalogoResponse> obtenerServiciosPorCategoria(Integer idCatTipoServicio);

    List<ProductoFormularioResponse> obtenerProductosPorServicio(Integer idServicio);

    GestoPagoProducto obtenerProductoSoportadoPorId(Integer idProducto);

    Optional<GestoPagoProducto> buscarProductoVerificacionPorServicio(Integer idServicio);
}
