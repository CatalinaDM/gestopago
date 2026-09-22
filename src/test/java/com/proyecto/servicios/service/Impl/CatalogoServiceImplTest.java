package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.CatalogoCacheService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceImplTest {

    @Mock
    private GestoPagoTokenService tokenService;

    @Mock
    private GestoPagoProductClient productClient;

    @Mock
    private GestoPagoProductoRepository productoRepository;

    @Mock
    private CatalogoCacheService cacheService;

    private CatalogoServiceImpl catalogoService;

    @BeforeEach
    void setUp() {
        catalogoService = new CatalogoServiceImpl(
                tokenService,
                productClient,
                productoRepository,
                cacheService,
                Mappers.getMapper(GestoPagoProductoMapper.class)
        );
        ReflectionTestUtils.setField(catalogoService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(catalogoService, "codigoDispositivo", "GPS83-TPV-17");
    }

    @Test
    void debeGuardarProductoYActualizarCacheCuandoLaRespuestaEsExitosa() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token-prueba");
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.of(token));
        when(productClient.getProductList("Bearer token-prueba"))
                .thenReturn(xmlConProductos(1));
        when(productoRepository.findAll()).thenReturn(List.of());

        catalogoService.actualizarCatalogo();

        ArgumentCaptor<List<GestoPagoProducto>> productosGuardados =
                ArgumentCaptor.forClass(List.class);
        verify(productoRepository).saveAll(productosGuardados.capture());
        verify(cacheService).guardarCatalogo(productosGuardados.getValue());
        assertEquals(1, productosGuardados.getValue().size());
    }

    @Test
    void noDebeGuardarCuandoLaRespuestaNoTieneProductos() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token-prueba");
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.of(token));
        when(productClient.getProductList("Bearer token-prueba"))
                .thenReturn(xmlSinProductos());

        catalogoService.actualizarCatalogo();

        verify(productoRepository, never()).findAll();
        verify(productoRepository, never()).saveAll(anyList());
        verify(cacheService, never()).guardarCatalogo(anyList());
    }

    @Test
    void noDebeActualizarCuandoElCatalogoNuevoEsMenor() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token-prueba");
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.of(token));
        when(productClient.getProductList("Bearer token-prueba"))
                .thenReturn(xmlConProductos(1));
        when(productoRepository.findAll()).thenReturn(List.of(producto(1), producto(2)));

        catalogoService.actualizarCatalogo();

        verify(productoRepository, never()).saveAll(anyList());
        verify(productoRepository, never()).deleteAllInBatch(anyList());
        verify(cacheService, never()).guardarCatalogo(anyList());
    }

        @Test
        void noDebeGuardarCuandoFallaLaComunicacionConGestoPago() {
                prepararToken();
                when(productClient.getProductList("Bearer token-prueba"))
                                .thenThrow(new RuntimeException("timeout"));

                catalogoService.actualizarCatalogo();

                verify(productoRepository, never()).findAll();
                verify(productoRepository, never()).saveAll(anyList());
                verify(cacheService, never()).guardarCatalogo(anyList());
        }

        @Test
        void noDebeGuardarCuandoElXmlEsInvalido() {
                prepararToken();
                when(productClient.getProductList("Bearer token-prueba"))
                                .thenReturn("<RESPONSE>");

                catalogoService.actualizarCatalogo();

                verify(productoRepository, never()).findAll();
                verify(productoRepository, never()).saveAll(anyList());
                verify(cacheService, never()).guardarCatalogo(anyList());
        }

        @Test
        void debeGuardarProductoNuevo() {
                prepararToken();
                when(productClient.getProductList("Bearer token-prueba"))
                                .thenReturn(xmlConProductos(1));
                when(productoRepository.findAll()).thenReturn(List.of());

                catalogoService.actualizarCatalogo();

                verify(productoRepository).saveAll(anyList());
        }

        @Test
        void noDebeGuardarProductoSinCambios() {
                prepararToken();
                when(productClient.getProductList("Bearer token-prueba"))
                                .thenReturn(xmlConProductos(1));
                when(productoRepository.findAll()).thenReturn(List.of(productoCompleto(1)));

                catalogoService.actualizarCatalogo();

                verify(productoRepository, never()).saveAll(anyList());
                verify(cacheService).guardarCatalogo(anyList());
        }

        @Test
        void debeGuardarProductoModificado() {
                prepararToken();
                when(productClient.getProductList("Bearer token-prueba"))
                                .thenReturn(xmlConProductos(1));
                GestoPagoProducto producto = productoCompleto(1);
                producto.setPrecio("20.0");
                when(productoRepository.findAll()).thenReturn(List.of(producto));

                catalogoService.actualizarCatalogo();

                verify(productoRepository).saveAll(anyList());
        }

        @Test
        void debeDevolverCodigoCuandoLaRespuestaNoTieneMensaje() {
                prepararTokenParaConsulta();
                when(productClient.getProductList("Bearer token-prueba"))
                                .thenReturn("<RESPONSE><PRODUCTOS></PRODUCTOS></RESPONSE>");

                CatalogoException exception = assertThrows(
                                CatalogoException.class,
                                () -> catalogoService.consultarCatalogoExterno());

                assertEquals("CATALOGO-007", exception.getCodigo());
        }

        @Test
        void debeDevolverCodigoCuandoGestoPagoRechazaLaConsulta() {
                prepararTokenParaConsulta();
                when(productClient.getProductList("Bearer token-prueba"))
                                .thenReturn(xmlConCodigo("08"));

                CatalogoException exception = assertThrows(
                                CatalogoException.class,
                                () -> catalogoService.consultarCatalogoExterno());

                assertEquals("CATALOGO-008", exception.getCodigo());
        }

        @Test
        void debeDevolverCodigoCuandoLaRespuestaNoTieneProductos() {
                prepararTokenParaConsulta();
                when(productClient.getProductList("Bearer token-prueba"))
                                .thenReturn(xmlSinProductos());

                CatalogoException exception = assertThrows(
                                CatalogoException.class,
                                () -> catalogoService.consultarCatalogoExterno());

                assertEquals("CATALOGO-009", exception.getCodigo());
        }

        @Test
        void debeDevolverCodigoCuandoNoExisteTokenParaLaConsulta() {
                when(tokenService.obtenerTokenParaConsulta(83, "GPS83-TPV-17"))
                                .thenReturn(Optional.empty());

                CatalogoException exception = assertThrows(
                                CatalogoException.class,
                                () -> catalogoService.consultarCatalogoExterno());

                assertEquals("AUTH-005", exception.getCodigo());
        }

        private void prepararToken() {
                GestoPagoToken token = new GestoPagoToken();
                token.setToken("token-prueba");
                when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17"))
                                .thenReturn(Optional.of(token));
        }

        private void prepararTokenParaConsulta() {
                when(tokenService.obtenerTokenParaConsulta(83, "GPS83-TPV-17"))
                                .thenReturn(Optional.of("token-prueba"));
        }

    private GestoPagoProducto producto(int idProducto) {
        GestoPagoProducto producto = new GestoPagoProducto();
        producto.setIdServicio(56);
        producto.setIdProducto(idProducto);
        return producto;
    }

        private GestoPagoProducto productoCompleto(int idProducto) {
                GestoPagoProducto producto = producto(idProducto);
                producto.setIdCatTipoServicio(15);
                producto.setProducto("Producto1");
                producto.setServicio("Servicio");
                producto.setTipoFront(2);
                producto.setHasDigitoVerificador(false);
                producto.setTipoReferencia("c");
                producto.setPrecio("10.0");
                producto.setShowAyuda(false);
                producto.setLegend("Ayuda");
                return producto;
        }

    private String xmlConProductos(int cantidad) {
        StringBuilder productos = new StringBuilder();
        for (int indice = 1; indice <= cantidad; indice++) {
            productos.append("<producto servicio=\"Servicio\" producto=\"Producto")
                    .append(indice)
                    .append("\" idServicio=\"56\" idProducto=\"")
                    .append(indice)
                    .append("\" idCatTipoServicio=\"15\" tipoFront=\"2\" ")
                    .append("hasDigitoVerificador=\"false\" precio=\"10.0\" ")
                    .append("showAyuda=\"false\" tipoReferencia=\"c\">")
                    .append("<legend>Ayuda</legend></producto>");
        }

        return "<RESPONSE><MENSAJE><CODIGO>01</CODIGO>"
                + "<TEXTO>Operacion realizada con exito</TEXTO></MENSAJE>"
                + "<PRODUCTOS>" + productos + "</PRODUCTOS></RESPONSE>";
    }

    private String xmlSinProductos() {
        return "<RESPONSE><MENSAJE><CODIGO>01</CODIGO>"
                + "<TEXTO>Operacion realizada con exito</TEXTO></MENSAJE>"
                + "<PRODUCTOS></PRODUCTOS></RESPONSE>";
    }

        private String xmlConCodigo(String codigo) {
                return "<RESPONSE><MENSAJE><CODIGO>" + codigo + "</CODIGO>"
                                + "<TEXTO>Operacion rechazada</TEXTO></MENSAJE>"
                                + "<PRODUCTOS></PRODUCTOS></RESPONSE>";
        }
}
