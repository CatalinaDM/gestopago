package com.proyecto.servicios.controller;

import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoResponse;
import com.proyecto.servicios.service.CatalogoConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/catalogo")
@Tag(name = "Catálogo de Servicios GestoPago", description = "Endpoints para consulta de productos y servicios externos disponibles en GestoPago.")
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
    @Operation(
            summary = "Consultar catálogo de productos disponibles",
            description = "Devuelve la lista sincronizada de productos y servicios activos provistos por el integrador GestoPago."
    )
    public ResponseEntity<List<GestoPagoProductoResponse>> obtenerProductos() {
        return ResponseEntity.ok(productoMapper.toResponseList(catalogoConsultaService.obtenerCatalogo()));
    }
}
