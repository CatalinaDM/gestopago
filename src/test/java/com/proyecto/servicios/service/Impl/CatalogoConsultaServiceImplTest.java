package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.CatalogoCacheService;
import com.proyecto.servicios.service.CatalogoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CatalogoConsultaServiceImplTest {

    @Mock
    private CatalogoCacheService cacheService;

    @Mock
    private GestoPagoProductoRepository productoRepository;

    @Mock
    private CatalogoService catalogoService;

    private CatalogoConsultaServiceImpl consultaService;

    @BeforeEach
    void setUp() {
        consultaService = new CatalogoConsultaServiceImpl(cacheService, productoRepository, catalogoService);
    }

    @Test
    void debeDevolverCatalogoDesdeRedisSinConsultarPostgreSQL() {
        List<GestoPagoProducto> productos = List.of(producto());
        when(cacheService.obtenerCatalogo()).thenReturn(Optional.of(productos));

        List<GestoPagoProducto> resultado = consultaService.obtenerCatalogo();

        assertEquals(productos, resultado);
        verify(productoRepository, never()).findAll();
    }

    @Test
    void debeConsultarPostgreSQLYRepoblarRedisCuandoHayMiss() {
        List<GestoPagoProducto> productos = List.of(producto());
        when(cacheService.obtenerCatalogo()).thenReturn(Optional.empty());
        when(productoRepository.findAll()).thenReturn(productos);

        List<GestoPagoProducto> resultado = consultaService.obtenerCatalogo();

        assertEquals(productos, resultado);
        verify(productoRepository).findAll();
        verify(cacheService).guardarCatalogo(productos);
    }

    @Test
    void debeConsultarGestoPagoCuandoPostgreSQLNoEstaDisponible() {
        List<GestoPagoProducto> productos = List.of(producto());
        when(cacheService.obtenerCatalogo()).thenReturn(Optional.empty());
        when(productoRepository.findAll()).thenThrow(new org.springframework.dao.DataRetrievalFailureException("caida"));
        when(catalogoService.consultarCatalogoExterno()).thenReturn(Optional.of(productos));

        List<GestoPagoProducto> resultado = consultaService.obtenerCatalogo();

        assertEquals(productos, resultado);
        verify(catalogoService).consultarCatalogoExterno();
        verify(cacheService).guardarCatalogo(productos);
    }

    private GestoPagoProducto producto() {
        GestoPagoProducto producto = new GestoPagoProducto();
        producto.setIdServicio(56);
        producto.setIdProducto(185);
        producto.setProducto("Recarga");
        return producto;
    }
}
