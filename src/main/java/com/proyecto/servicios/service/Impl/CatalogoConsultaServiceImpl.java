package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.enums.ModoCaptura;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.exception.ProductoNoEncontradoException;
import com.proyecto.servicios.exception.ProductoNoSoportadoException;
import com.proyecto.servicios.model.catalogo.CategoriaCatalogoResponse;
import com.proyecto.servicios.model.catalogo.ProductoFormularioResponse;
import com.proyecto.servicios.model.catalogo.ServicioCatalogoResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.CatalogoCacheService;
import com.proyecto.servicios.service.CatalogoClasificadorComponent;
import com.proyecto.servicios.service.CatalogoConsultaService;
import com.proyecto.servicios.service.CatalogoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CatalogoConsultaServiceImpl implements CatalogoConsultaService {

    private final CatalogoCacheService catalogoCacheService;
    private final GestoPagoProductoRepository productoRepository;
    private final CatalogoService catalogoService;
    private final CatalogoClasificadorComponent clasificadorComponent;

    public CatalogoConsultaServiceImpl(
            CatalogoCacheService catalogoCacheService,
            GestoPagoProductoRepository productoRepository,
            CatalogoService catalogoService) {
        this(catalogoCacheService, productoRepository, catalogoService, new CatalogoClasificadorComponent());
    }

    @Autowired
    public CatalogoConsultaServiceImpl(
            CatalogoCacheService catalogoCacheService,
            GestoPagoProductoRepository productoRepository,
            CatalogoService catalogoService,
            CatalogoClasificadorComponent clasificadorComponent) {
        this.catalogoCacheService = catalogoCacheService;
        this.productoRepository = productoRepository;
        this.catalogoService = catalogoService;
        this.clasificadorComponent = clasificadorComponent;
    }

    @Override
    public List<GestoPagoProducto> obtenerCatalogo() {
        var catalogoEnCache = catalogoCacheService.obtenerCatalogo();

        if (catalogoEnCache.isPresent()) {
            log.debug("Catálogo obtenido desde Redis");
            return asegurarClasificacion(catalogoEnCache.get());
        }

        log.debug("Catálogo no disponible en Redis. Consultando PostgreSQL");
        try {
            List<GestoPagoProducto> catalogoEnBaseDeDatos = asegurarClasificacion(productoRepository.findAll());
            catalogoCacheService.guardarCatalogo(catalogoEnBaseDeDatos);
            return catalogoEnBaseDeDatos;
        } catch (DataAccessException e) {
            log.warn("CATALOGO-003: PostgreSQL no está disponible. Consultando GestoPago como fallback");
            List<GestoPagoProducto> catalogoExterno = catalogoService.consultarCatalogoExterno()
                    .orElseThrow(() -> new CatalogoException(
                            "CATALOGO-003",
                            "No fue posible consultar PostgreSQL ni obtener el catálogo externo",
                            HttpStatus.SERVICE_UNAVAILABLE));
            asegurarClasificacion(catalogoExterno);
            catalogoCacheService.guardarCatalogo(catalogoExterno);
            return catalogoExterno;
        }
    }

    /**
     * Obtiene el catálogo local exclusivamente desde Redis o PostgreSQL.
     * Nunca invoca getProductList para proteger el límite diario del proveedor.
     */
    private List<GestoPagoProducto> obtenerCatalogoLocalExclusivo() {
        var catalogoEnCache = catalogoCacheService.obtenerCatalogo();
        if (catalogoEnCache.isPresent() && !catalogoEnCache.get().isEmpty()) {
            return asegurarClasificacion(catalogoEnCache.get());
        }

        try {
            List<GestoPagoProducto> catalogoEnBaseDeDatos = asegurarClasificacion(productoRepository.findAll());
            if (!catalogoEnBaseDeDatos.isEmpty()) {
                catalogoCacheService.guardarCatalogo(catalogoEnBaseDeDatos);
            }
            return catalogoEnBaseDeDatos;
        } catch (DataAccessException e) {
            log.error("CATALOGO-003: Error al consultar el catálogo local en PostgreSQL");
            throw new CatalogoException(
                    "CATALOGO-003",
                    "El catálogo local de productos no está disponible en este momento",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private List<GestoPagoProducto> asegurarClasificacion(List<GestoPagoProducto> productos) {
        if (productos == null) {
            return List.of();
        }
        productos.forEach(p -> {
            if (p.getModoCaptura() == null || p.getModoCaptura() == ModoCaptura.NO_SOPORTADO) {
                p.setModoCaptura(clasificadorComponent.clasificar(p));
            }
        });
        return productos;
    }

    private List<GestoPagoProducto> obtenerProductosSoportados() {
        return obtenerCatalogoLocalExclusivo().stream()
                .filter(p -> p.getModoCaptura() != null && p.getModoCaptura() != ModoCaptura.NO_SOPORTADO)
                .toList();
    }

    @Override
    public List<CategoriaCatalogoResponse> obtenerCategorias() {
        log.info("Consultando categorías soportadas desde catálogo local");
        Map<Integer, Long> serviciosPorCategoria = obtenerProductosSoportados().stream()
                .filter(p -> p.getIdCatTipoServicio() != null && p.getIdServicio() != null)
                .collect(Collectors.groupingBy(
                        GestoPagoProducto::getIdCatTipoServicio,
                        Collectors.mapping(GestoPagoProducto::getIdServicio,
                                Collectors.collectingAndThen(Collectors.toSet(), s -> (long) s.size()))
                ));

        return serviciosPorCategoria.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> CategoriaCatalogoResponse.builder()
                        .idCatTipoServicio(entry.getKey())
                        .nombre(clasificadorComponent.obtenerNombreCategoria(entry.getKey()))
                        .totalServicios(entry.getValue().intValue())
                        .build())
                .toList();
    }

    @Override
    public List<ServicioCatalogoResponse> obtenerServiciosPorCategoria(Integer idCatTipoServicio) {
        log.info("Consultando servicios locales para la categoría: {}", idCatTipoServicio);
        Map<Integer, ServicioCatalogoResponse> serviciosUnicos = new LinkedHashMap<>();

        obtenerProductosSoportados().stream()
                .filter(p -> Objects.equals(p.getIdCatTipoServicio(), idCatTipoServicio))
                .filter(p -> p.getIdServicio() != null)
                .sorted(Comparator.comparing(p -> p.getServicio() != null ? p.getServicio() : ""))
                .forEach(p -> serviciosUnicos.putIfAbsent(
                        p.getIdServicio(),
                        ServicioCatalogoResponse.builder()
                                .idServicio(p.getIdServicio())
                                .servicio(p.getServicio())
                                .idCatTipoServicio(p.getIdCatTipoServicio())
                                .build()
                ));

        return List.copyOf(serviciosUnicos.values());
    }

    @Override
    public List<ProductoFormularioResponse> obtenerProductosPorServicio(Integer idServicio) {
        log.info("Consultando productos soportados locales para el servicio: {}", idServicio);
        return obtenerProductosSoportados().stream()
                .filter(p -> Objects.equals(p.getIdServicio(), idServicio))
                .map(clasificadorComponent::toProductoFormularioResponse)
                .toList();
    }

    @Override
    public GestoPagoProducto obtenerProductoSoportadoPorId(Integer idProducto) {
        if (idProducto == null) {
            throw new ProductoNoEncontradoException("El idProducto es obligatorio");
        }

        GestoPagoProducto producto = obtenerCatalogoLocalExclusivo().stream()
                .filter(p -> Objects.equals(p.getIdProducto(), idProducto))
                .findFirst()
                .orElseGet(() -> productoRepository.findFirstByIdProducto(idProducto)
                        .map(p -> {
                            if (p.getModoCaptura() == null || p.getModoCaptura() == ModoCaptura.NO_SOPORTADO) {
                                p.setModoCaptura(clasificadorComponent.clasificar(p));
                            }
                            return p;
                        })
                        .orElseThrow(() -> new ProductoNoEncontradoException(idProducto)));

        if (producto.getModoCaptura() == null || producto.getModoCaptura() == ModoCaptura.NO_SOPORTADO) {
            throw new ProductoNoSoportadoException(idProducto);
        }

        return producto;
    }

    @Override
    public Optional<GestoPagoProducto> buscarProductoVerificacionPorServicio(Integer idServicio) {
        if (idServicio == null) {
            return Optional.empty();
        }
        return obtenerCatalogoLocalExclusivo().stream()
                .filter(p -> Objects.equals(p.getIdServicio(), idServicio))
                .filter(p -> Objects.equals(p.getTipoFront(), 4))
                .findFirst();
    }
}
