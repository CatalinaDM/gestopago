package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.CatalogoService;
import com.proyecto.servicios.service.CatalogoCacheService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.proyecto.servicios.model.gestopago.GestoPagoProductResponse;
import feign.FeignException;
import feign.RetryableException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import java.util.Objects;

import java.io.StringReader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Optional;

@Service
@Slf4j
public class CatalogoServiceImpl implements CatalogoService {

    private final GestoPagoTokenService gestoPagoTokenService;
    private final GestoPagoProductClient gestoPagoProductClient;
        private final GestoPagoProductoRepository productoRepository;
        private final CatalogoCacheService catalogoCacheService;
        private final GestoPagoProductoMapper productoMapper;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    public CatalogoServiceImpl(
            GestoPagoTokenService gestoPagoTokenService,
            GestoPagoProductClient gestoPagoProductClient,
            GestoPagoProductoRepository productoRepository,
            CatalogoCacheService catalogoCacheService,
            GestoPagoProductoMapper productoMapper) {

        this.gestoPagoTokenService = gestoPagoTokenService;
        this.gestoPagoProductClient = gestoPagoProductClient;
        this.productoRepository = productoRepository;
        this.catalogoCacheService = catalogoCacheService;
        this.productoMapper = productoMapper;
    }

    @Override
    @Scheduled(cron = "${gestopago.productos.sync.cron}")
    public void actualizarCatalogo() {
        log.info("Iniciando actualización del catálogo de GestoPago");

        try {
            ejecutarActualizacion();
        } catch (DataAccessException e) {
            log.error("CATALOGO-003: error de persistencia en PostgreSQL");
        } catch (Exception e) {
            log.error("CATALOGO-999: error inesperado de integración ({})",
                    e.getClass().getSimpleName());
        } finally {
            log.info("Finalizó la actualización del catálogo de GestoPago");
        }
    }

    @Override
    public Optional<List<GestoPagoProducto>> consultarCatalogoExterno() {
        var tokenOptional = gestoPagoTokenService.obtenerTokenParaConsulta(
                idDistribuidor,
                codigoDispositivo
        );

        if (tokenOptional.isEmpty()) {
            throw new CatalogoException(
                    "AUTH-005",
                    "No existe un token válido para consultar GestoPago",
                    HttpStatus.BAD_GATEWAY);
        }

        try {
            String responseXml = gestoPagoProductClient.getProductList(
                    "Bearer " + tokenOptional.get()
            );
            GestoPagoProductResponse response = convertirXml(responseXml);

            if (response.getMensaje() == null) {
                throw new CatalogoException(
                        "CATALOGO-007",
                        "GestoPago no devolvió la sección MENSAJE",
                        HttpStatus.BAD_GATEWAY);
            }
            if (!"01".equals(response.getMensaje().getCodigo())) {
                throw new CatalogoException(
                        "CATALOGO-008",
                        "GestoPago rechazó la consulta del catálogo",
                        HttpStatus.BAD_GATEWAY);
            }
            if (response.getProductos() == null || response.getProductos().isEmpty()) {
                throw new CatalogoException(
                        "CATALOGO-009",
                        "GestoPago respondió sin productos",
                        HttpStatus.BAD_GATEWAY);
            }

            List<GestoPagoProducto> productos = response.getProductos().stream()
                    .map(productoMapper::toEntity)
                    .toList();
            log.info("Catálogo obtenido directamente desde GestoPago. Productos: {}", productos.size());
            return Optional.of(productos);
            } catch (CatalogoException e) {
                throw e;
        } catch (RetryableException e) {
            log.error("CATALOGO-001: timeout o error de conexión con GestoPago");
            throw new CatalogoException(
                    "CATALOGO-001",
                    "GestoPago no está disponible o agotó el tiempo de espera",
                    HttpStatus.SERVICE_UNAVAILABLE);
        } catch (FeignException e) {
            if (e.status() == 401 || e.status() == 403) {
                throw new CatalogoException(
                        "CATALOGO-002",
                        "GestoPago rechazó la autenticación",
                        HttpStatus.BAD_GATEWAY);
            }
            throw new CatalogoException(
                    "CATALOGO-004",
                    "GestoPago respondió con un error HTTP",
                    HttpStatus.BAD_GATEWAY);
        } catch (JAXBException e) {
            log.error("CATALOGO-006: respuesta XML inválida de GestoPago");
            throw new CatalogoException(
                    "CATALOGO-006",
                    "GestoPago devolvió una respuesta XML inválida",
                    HttpStatus.BAD_GATEWAY);
        } catch (Exception e) {
            log.error("CATALOGO-005: error de comunicación con GestoPago ({})",
                    e.getClass().getSimpleName());
            throw new CatalogoException(
                    "CATALOGO-005",
                    "Ocurrió un error al comunicarse con GestoPago",
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private void ejecutarActualizacion() {

        var tokenOptional = gestoPagoTokenService.obtenerTokenActivo(
                idDistribuidor,
                codigoDispositivo
        );

        if (tokenOptional.isEmpty()) {
            log.error("AUTH-005: no se encontró un token activo de GestoPago");
            return;
        }

        String responseXml;
        try {
            responseXml = gestoPagoProductClient.getProductList(
                    "Bearer " + tokenOptional.get().getToken()
            );
            log.info("Respuesta de catálogo de GestoPago recibida");
        } catch (RetryableException e) {
            log.error("CATALOGO-001: timeout o error de conexión con GestoPago");
            return;
        } catch (FeignException e) {
            if (e.status() == 401 || e.status() == 403) {
                log.error("CATALOGO-002: GestoPago rechazó la autenticación. HTTP {}", e.status());
            } else {
                log.error("CATALOGO-004: GestoPago respondió con HTTP {}", e.status());
            }
            return;
        } catch (Exception e) {
            log.error("CATALOGO-005: error de comunicación con GestoPago ({})",
                    e.getClass().getSimpleName());
            return;
        }

        GestoPagoProductResponse response;
        try {
            response = convertirXml(responseXml);
        } catch (JAXBException e) {
            log.error("CATALOGO-006: respuesta XML inválida de GestoPago");
            return;
        }

        if (!respuestaValida(response)) {
            return;
        }

        var productosActuales = productoRepository.findAll();
        var productosNuevos = response.getProductos();
        long cantidadActual = productosActuales.size();
        long cantidadNueva = productosNuevos.size();

        log.info("Comparando catálogo actual ({}) contra catálogo nuevo ({})",
                cantidadActual, cantidadNueva);

        if (cantidadNueva < cantidadActual) {
            log.warn("El catálogo nuevo contiene menos productos que el catálogo actual. No se actualizará.");
            return;
        }

        Map<String, GestoPagoProducto> productosActualesMap = productosActuales.stream()
                .collect(Collectors.toMap(this::claveProducto, producto -> producto));
        List<GestoPagoProducto> productosParaGuardar = new ArrayList<>();
        List<GestoPagoProducto> catalogoParaCache = new ArrayList<>(productosActuales);
        Set<String> clavesRecibidas = new HashSet<>();

        for (var productoDto : productosNuevos) {
            clavesRecibidas.add(productoDto.getIdServicio() + "-" + productoDto.getIdProducto());
            procesarProducto(productoDto, productosActualesMap,
                    productosParaGuardar, catalogoParaCache);
        }

        List<GestoPagoProducto> productosObsoletos = productosActuales.stream()
            .filter(producto -> !clavesRecibidas.contains(claveProducto(producto)))
            .toList();

        if (!productosObsoletos.isEmpty()) {
            productoRepository.deleteAllInBatch(productosObsoletos);
            catalogoParaCache.removeIf(producto -> !clavesRecibidas.contains(claveProducto(producto)));
        }

        if (!productosParaGuardar.isEmpty()) {
            productoRepository.saveAll(productosParaGuardar);
        }
        catalogoCacheService.guardarCatalogo(catalogoParaCache);
        log.info("Catálogo de GestoPago actualizado correctamente. Productos procesados: {}",
                cantidadNueva);
    }

    private boolean respuestaValida(GestoPagoProductResponse response) {
        if (response.getMensaje() == null) {
            log.error("CATALOGO-007: respuesta de GestoPago sin sección MENSAJE");
            return false;
        }

        if (!"01".equals(response.getMensaje().getCodigo())) {
                log.error("CATALOGO-008: GestoPago respondió con código {}",
                        response.getMensaje().getCodigo());
            return false;
        }

        if (response.getProductos() == null || response.getProductos().isEmpty()) {
            log.warn("CATALOGO-009: GestoPago respondió correctamente, pero no se recibieron productos");
            return false;
        }

        log.info("Respuesta GestoPago exitosa con código 01");
        log.info("Productos recibidos: {}", response.getProductos().size());
        return true;
    }

    private void procesarProducto(
            com.proyecto.servicios.model.gestopago.GestoPagoProducto productoDto,
            Map<String, GestoPagoProducto> productosActualesMap,
            List<GestoPagoProducto> productosParaGuardar,
            List<GestoPagoProducto> catalogoParaCache) {

        String clave = productoDto.getIdServicio() + "-" + productoDto.getIdProducto();
        GestoPagoProducto productoEntity = productosActualesMap.get(clave);

        if (productoEntity == null) {
            productoEntity = productoMapper.toEntity(productoDto);
            productosActualesMap.put(clave, productoEntity);
            catalogoParaCache.add(productoEntity);
            productosParaGuardar.add(productoEntity);
            return;
        }

        if (productoCambio(productoEntity, productoDto)) {
            productoMapper.updateEntity(productoDto, productoEntity);
            productosParaGuardar.add(productoEntity);
        }
    }

    private boolean productoCambio(
            GestoPagoProducto productoEntity,
            com.proyecto.servicios.model.gestopago.GestoPagoProducto productoDto) {
        return !Objects.equals(productoEntity.getIdCatTipoServicio(), productoDto.getIdCatTipoServicio())
                || !Objects.equals(productoEntity.getProducto(), productoDto.getProducto())
                || !Objects.equals(productoEntity.getServicio(), productoDto.getServicio())
                || !Objects.equals(productoEntity.getTipoFront(), productoDto.getTipoFront())
                || !Objects.equals(productoEntity.getHasDigitoVerificador(), productoDto.getHasDigitoVerificador())
                || !Objects.equals(productoEntity.getTipoReferencia(), productoDto.getTipoReferencia())
                || !Objects.equals(productoEntity.getPrecio(), productoDto.getPrecio())
                || !Objects.equals(productoEntity.getShowAyuda(), productoDto.getShowAyuda())
                || !Objects.equals(productoEntity.getLegend(), productoDto.getLegend());
    }

    private String claveProducto(GestoPagoProducto producto) {
        return producto.getIdServicio() + "-" + producto.getIdProducto();
    }

    private GestoPagoProductResponse convertirXml(String xml) throws JAXBException {

        JAXBContext context =
                JAXBContext.newInstance(GestoPagoProductResponse.class);

        Unmarshaller unmarshaller = context.createUnmarshaller();

        return (GestoPagoProductResponse) unmarshaller.unmarshal(
                new StringReader(xml)
        );
    }
}