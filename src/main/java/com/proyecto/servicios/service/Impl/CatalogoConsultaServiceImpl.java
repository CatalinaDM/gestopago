package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.CatalogoCacheService;
import com.proyecto.servicios.service.CatalogoConsultaService;
import com.proyecto.servicios.service.CatalogoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CatalogoConsultaServiceImpl implements CatalogoConsultaService {

    private final CatalogoCacheService catalogoCacheService;
    private final GestoPagoProductoRepository productoRepository;
    private final CatalogoService catalogoService;

    public CatalogoConsultaServiceImpl(
            CatalogoCacheService catalogoCacheService,
            GestoPagoProductoRepository productoRepository,
            CatalogoService catalogoService) {
        this.catalogoCacheService = catalogoCacheService;
        this.productoRepository = productoRepository;
        this.catalogoService = catalogoService;
    }

    @Override
    public List<GestoPagoProducto> obtenerCatalogo() {
        var catalogoEnCache = catalogoCacheService.obtenerCatalogo();

        if (catalogoEnCache.isPresent()) {
            log.debug("Catálogo obtenido desde Redis");
            return catalogoEnCache.get();
        }

        log.debug("Catálogo no disponible en Redis. Consultando PostgreSQL");
        try {
            List<GestoPagoProducto> catalogoEnBaseDeDatos = productoRepository.findAll();
            catalogoCacheService.guardarCatalogo(catalogoEnBaseDeDatos);
            return catalogoEnBaseDeDatos;
        } catch (DataAccessException e) {
            log.warn("CATALOGO-003: PostgreSQL no está disponible. Consultando GestoPago como fallback");
            List<GestoPagoProducto> catalogoExterno = catalogoService.consultarCatalogoExterno()
                    .orElseThrow(() -> new CatalogoException(
                        "CATALOGO-003",
                        "No fue posible consultar PostgreSQL ni obtener el catálogo externo",
                        HttpStatus.SERVICE_UNAVAILABLE));
            catalogoCacheService.guardarCatalogo(catalogoExterno);
            return catalogoExterno;
        }
    }
}
