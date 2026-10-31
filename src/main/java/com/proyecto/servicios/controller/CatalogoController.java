package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.catalogo.CategoriaCatalogoRequest;
import com.proyecto.servicios.model.catalogo.CategoriaCatalogoResponse;
import com.proyecto.servicios.model.catalogo.ProductoFormularioResponse;
import com.proyecto.servicios.model.catalogo.ProductosPorServicioRequest;
import com.proyecto.servicios.model.catalogo.ServicioCatalogoResponse;
import com.proyecto.servicios.model.catalogo.ServiciosPorCategoriaRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoResponse;
import com.proyecto.servicios.service.CatalogoConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/catalogo")
@Tag(name = "Catálogo de Servicios GestoPago", description = "Endpoints para consulta local de categorías, servicios y productos soportados en GestoPago.")
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
            summary = "Consultar catálogo completo sincronizado",
            description = "Devuelve la lista sincronizada de productos y servicios activos provistos por el integrador GestoPago."
    )
    public ResponseEntity<List<GestoPagoProductoResponse>> obtenerProductos() {
        return ResponseEntity.ok(productoMapper.toResponseList(catalogoConsultaService.obtenerCatalogo()));
    }

    @PostMapping("/categorias")
    @Operation(
            summary = "Consultar categorías de servicios soportadas",
            description = "Lee del catálogo local (Redis/BD) las categorías disponibles con productos soportados."
    )
    public ResponseEntity<List<CategoriaCatalogoResponse>> obtenerCategorias(
            @Valid @RequestBody(required = false) GenericRequest<CategoriaCatalogoRequest> request) {
        return ResponseEntity.ok(catalogoConsultaService.obtenerCategorias());
    }

    @PostMapping("/servicios")
    @Operation(
            summary = "Consultar servicios por categoría",
            description = "Devuelve los servicios disponibles dentro de la categoría (idCatTipoServicio) seleccionada."
    )
    public ResponseEntity<List<ServicioCatalogoResponse>> obtenerServiciosPorCategoria(
            @Valid @RequestBody GenericRequest<ServiciosPorCategoriaRequest> request) {
        if (request == null || request.getDatos() == null || request.getDatos().getIdCatTipoServicio() == null) {
            throw new ValidacionException("VALIDACION-001", "El idCatTipoServicio dentro de 'datos' es obligatorio");
        }
        return ResponseEntity.ok(catalogoConsultaService.obtenerServiciosPorCategoria(
                request.getDatos().getIdCatTipoServicio()
        ));
    }

    @PostMapping("/productos")
    @Operation(
            summary = "Consultar productos soportados por servicio para el formulario de pago",
            description = "Devuelve los productos soportados de un servicio con sus metadatos de captura (requiereTelefono, requiereReferencia, requiereMonto, montoFijo, etiquetaCampo). Excluye NO_SOPORTADO y nunca expone tipoFront."
    )
    public ResponseEntity<List<ProductoFormularioResponse>> obtenerProductosPorServicio(
            @Valid @RequestBody GenericRequest<ProductosPorServicioRequest> request) {
        if (request == null || request.getDatos() == null || request.getDatos().getIdServicio() == null) {
            throw new ValidacionException("VALIDACION-001", "El idServicio dentro de 'datos' es obligatorio");
        }
        return ResponseEntity.ok(catalogoConsultaService.obtenerProductosPorServicio(
                request.getDatos().getIdServicio()
        ));
    }
}
