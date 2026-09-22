package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
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

    public CatalogoController(CatalogoConsultaService catalogoConsultaService) {
        this.catalogoConsultaService = catalogoConsultaService;
    }

    @GetMapping("/productos")
    public ResponseEntity<List<GestoPagoProducto>> obtenerProductos() {
        return ResponseEntity.ok(catalogoConsultaService.obtenerCatalogo());
    }
}
