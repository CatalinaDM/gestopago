package com.proyecto.servicios.controller;

import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoResponse;
import com.proyecto.servicios.service.CatalogoConsultaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/catalogo")
public class CatalogoController {

    private final CatalogoConsultaService catalogoConsultaService;
    private final GestoPagoProductoMapper productoMapper;

    public CatalogoController(
            CatalogoConsultaService catalogoConsultaService,
            GestoPagoProductoMapper productoMapper) {
        this.catalogoConsultaService = catalogoConsultaService;
        this.productoMapper = productoMapper;
    }

    @GetMapping("/productos")
    public ResponseEntity<List<GestoPagoProductoResponse>> obtenerProductos() {
        return ResponseEntity.ok(productoMapper.toResponseList(catalogoConsultaService.obtenerCatalogo()));
    }
}
