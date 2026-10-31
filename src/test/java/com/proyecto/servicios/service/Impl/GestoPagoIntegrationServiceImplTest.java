package com.proyecto.servicios.service.Impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.exception.GestoPagoProveedorRechazoException;
import com.proyecto.servicios.exception.GestoPagoTimeoutException;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxXmlResponse;
import com.proyecto.servicios.service.AesCipherService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoIntegrationServiceImplTest {

    private static final String TEST_SECRET_KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String TEST_IV = "p5ldmBPdd/9pjC0bDC/nSg==";

    @Mock
    private GestoPagoTxClient gestoPagoTxClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    private AesCipherService aesCipherService;
    private GestoPagoIntegrationServiceImpl integrationService;

    @BeforeEach
    void setUp() {
        aesCipherService = new AesCipherServiceImpl(TEST_SECRET_KEY, TEST_IV);
        integrationService = new GestoPagoIntegrationServiceImpl(
                gestoPagoTxClient,
                gestoPagoTokenService,
                aesCipherService,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(integrationService, "apiKey", "API-KEY-SECRETA-999");
    }

    @Test
    @DisplayName("Envía sendTx con éxito, parsea SALDO en notación científica y no expone datos sensibles en logs")
    void enviarTransaccion_ExitosoYParseaNotacionCientificaSinExponerSecretosEnLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(GestoPagoIntegrationServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        String tokenSecreto = "eyJhbGciOiJIUzI1NiJ9.tokenSecretoGestoPago";
        when(gestoPagoTokenService.obtenerTokenValido()).thenReturn(tokenSecreto);

        String xmlExito = "<RESPONSE>"
                + "<MENSAJE><CODIGO>01</CODIGO><TEXTO>Transaccion exitosa</TEXTO></MENSAJE>"
                + "<ID_TX>987654</ID_TX>"
                + "<NUM_AUTORIZACION>AUT-112233</NUM_AUTORIZACION>"
                + "<SALDO>1.25E4</SALDO>"
                + "<COMISION>10.00</COMISION>"
                + "<MONTO>100.00</MONTO>"
                + "<REFERENCIA>5512345678</REFERENCIA>"
                + "<ALFILER>PIN-9988-7766</ALFILER>"
                + "<FOLIO>FOL-5544</FOLIO>"
                + "<LEYENDA>Conserve su comprobante</LEYENDA>"
                + "</RESPONSE>";

        when(gestoPagoTxClient.sendTxConApiKey(eq("Bearer " + tokenSecreto), eq("API-KEY-SECRETA-999"), any()))
                .thenReturn(xmlExito);

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idServicio(56)
                .idProducto(185)
                .tipoFront(1)
                .telefono("5512345678")
                .horaLocal("20261026183000")
                .montoPago("100.00")
                .upc("ABC1234567")
                .unidad("1")
                .build();

        GestoPagoSendTxXmlResponse resp = integrationService.enviarTransaccion(payload);

        assertEquals("01", resp.obtenerCodigoEfectivo());
        assertEquals("987654", resp.getIdTx());
        assertEquals("AUT-112233", resp.getNumAutorizacion());
        assertEquals(0, new BigDecimal("12500").compareTo(resp.obtenerSaldoDecimal()));
        assertEquals("PIN-9988-7766", resp.obtenerPinEfectivo());
        assertEquals("FOL-5544", resp.obtenerFolioEfectivo());
        assertEquals("Conserve su comprobante", resp.obtenerLeyendaEfectiva());

        for (ILoggingEvent event : listAppender.list) {
            String msg = event.getFormattedMessage();
            assertFalse(msg.contains(tokenSecreto), "El log no debe contener el JWT de GestoPago");
            assertFalse(msg.contains("API-KEY-SECRETA-999"), "El log no debe contener el API Key");
            assertFalse(msg.contains("5512345678"), "El log no debe contener el payload sin cifrar");
        }

        logger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("Parsea correctamente el XML oficial de GestoPago con SALDO_F, COMISION_F y REFERENCIA dentro de MENSAJE")
    void enviarTransaccion_ParseaXmlOficialGestoPago() {
        ReflectionTestUtils.setField(integrationService, "apiKey", "");
        when(gestoPagoTokenService.obtenerTokenValido()).thenReturn("token-oficial");

        String xmlOficial = "<?xml version='1.0' encoding='UTF-8'?>\n"
                + "<RESPONSE>\n"
                + "    <ID_TX>291437188</ID_TX>\n"
                + "    <NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>\n"
                + "    <SALDO>157731.83</SALDO>\n"
                + "    <COMISION>0.7</COMISION>\n"
                + "    <SALDO_F>157,731.83</SALDO_F>\n"
                + "    <COMISION_F>0.7</COMISION_F>\n"
                + "    <FECHA>25/Feb/2022 18:15:25</FECHA>\n"
                + "    <MONTO>10.0</MONTO>\n"
                + "    <MENSAJE>\n"
                + "        <CODIGO>01</CODIGO>\n"
                + "        <TEXTO>Operacion realizada con exito</TEXTO>\n"
                + "        <REFERENCIA></REFERENCIA>\n"
                + "    </MENSAJE>\n"
                + "</RESPONSE>";

        when(gestoPagoTxClient.sendTx(eq("Bearer token-oficial"), any())).thenReturn(xmlOficial);

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idServicio(2284)
                .idProducto(14302)
                .tipoFront(1)
                .telefono("5512345678")
                .horaLocal("20261030015000")
                .montoPago("100.00")
                .upc("OFICIAL001")
                .unidad("1")
                .build();

        GestoPagoSendTxXmlResponse resp = integrationService.enviarTransaccion(payload);

        assertEquals("01", resp.obtenerCodigoEfectivo());
        assertEquals("Operacion realizada con exito", resp.obtenerTextoEfectivo());
        assertEquals("291437188", resp.getIdTx());
        assertEquals("100024988", resp.getNumAutorizacion());
        assertEquals("157,731.83", resp.getSaldoF());
        assertEquals("0.7", resp.getComisionF());
    }

    @Test
    @DisplayName("Parsea correctamente el XML <AUTORIZACION> con CODIGO_MENSAJE y MENSAJE plano devuelto por GestoPago")
    void enviarTransaccion_ParseaXmlAutorizacionConCodigoMensaje() {
        ReflectionTestUtils.setField(integrationService, "apiKey", "");
        when(gestoPagoTokenService.obtenerTokenValido()).thenReturn("token-oficial");

        String xmlAutorizacion = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<AUTORIZACION> "
                + "<NUM_AUTORIZACION>-1</NUM_AUTORIZACION>"
                + "<CODIGO_MENSAJE>03</CODIGO_MENSAJE>"
                + "<MENSAJE>Por el momento el carrier no responde, favor de intentar mas tarde.</MENSAJE>"
                + "</AUTORIZACION>";

        when(gestoPagoTxClient.sendTx(eq("Bearer token-oficial"), any())).thenReturn(xmlAutorizacion);

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idServicio(2284)
                .idProducto(14302)
                .tipoFront(1)
                .telefono("4181607273")
                .horaLocal("20261030023000")
                .montoPago("100.00")
                .upc("AUTXML0001")
                .unidad("1")
                .build();

        GestoPagoProveedorRechazoException ex = assertThrows(
                GestoPagoProveedorRechazoException.class,
                () -> integrationService.enviarTransaccion(payload)
        );
        assertEquals("03", ex.getCodigoProveedor());
        assertTrue(ex.getMessage().contains("Por el momento el carrier no responde"));
    }

    @Test
    @DisplayName("Ante HTTP 403 en sendTx, renueva el token una vez y reintenta exitosamente")
    void enviarTransaccion_RenuevaTokenAnte403YReintentaUnaSolaVez() {
        String tokenExpirado = "token-expirado-1";
        String tokenNuevo = "token-nuevo-2";

        when(gestoPagoTokenService.obtenerTokenValido()).thenReturn(tokenExpirado);
        when(gestoPagoTokenService.renovarYObtenerToken(tokenExpirado)).thenReturn(tokenNuevo);

        Request dummyReq = Request.create(Request.HttpMethod.POST, "/sistema/service/sendTx.do",
                Collections.emptyMap(), new byte[0], StandardCharsets.UTF_8, new RequestTemplate());
        FeignException.Forbidden forbidden403 = new FeignException.Forbidden(
                "403 Forbidden", dummyReq, new byte[0], Collections.emptyMap());

        String xmlExito = "<RESPONSE><MENSAJE><CODIGO>01</CODIGO><TEXTO>OK</TEXTO></MENSAJE><ID_TX>111</ID_TX></RESPONSE>";

        when(gestoPagoTxClient.sendTxConApiKey(eq("Bearer " + tokenExpirado), anyString(), any()))
                .thenThrow(forbidden403);
        when(gestoPagoTxClient.sendTxConApiKey(eq("Bearer " + tokenNuevo), anyString(), any()))
                .thenReturn(xmlExito);

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idServicio(1)
                .idProducto(2)
                .upc("RETRY00001")
                .build();

        GestoPagoSendTxXmlResponse resp = integrationService.enviarTransaccion(payload);

        assertEquals("01", resp.obtenerCodigoEfectivo());
        verify(gestoPagoTokenService, times(1)).renovarYObtenerToken(tokenExpirado);
        verify(gestoPagoTxClient, times(1)).sendTxConApiKey(eq("Bearer " + tokenExpirado), anyString(), any());
        verify(gestoPagoTxClient, times(1)).sendTxConApiKey(eq("Bearer " + tokenNuevo), anyString(), any());
    }

    @Test
    @DisplayName("Mapea CODIGO distinto de 01 a GestoPagoProveedorRechazoException")
    void enviarTransaccion_CodigoDistintoDe01LanzaExcepcionDeRechazo() {
        when(gestoPagoTokenService.obtenerTokenValido()).thenReturn("token-ok");
        String xmlRechazo = "<RESPONSE><MENSAJE><CODIGO>05</CODIGO><TEXTO>Referencia invalida</TEXTO></MENSAJE></RESPONSE>";
        when(gestoPagoTxClient.sendTxConApiKey(anyString(), anyString(), any())).thenReturn(xmlRechazo);

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idServicio(1)
                .idProducto(2)
                .upc("RECHAZO001")
                .build();

        GestoPagoProveedorRechazoException ex = assertThrows(
                GestoPagoProveedorRechazoException.class,
                () -> integrationService.enviarTransaccion(payload)
        );
        assertEquals("05", ex.getCodigoProveedor());
    }

    @Test
    @DisplayName("Mapea RetryableException (timeout) a GestoPagoTimeoutException")
    void enviarTransaccion_TimeoutLanzaGestoPagoTimeoutException() {
        when(gestoPagoTokenService.obtenerTokenValido()).thenReturn("token-ok");
        Request dummyReq = Request.create(Request.HttpMethod.POST, "/sistema/service/sendTx.do",
                Collections.emptyMap(), new byte[0], StandardCharsets.UTF_8, new RequestTemplate());
        RetryableException timeoutEx = new RetryableException(
                -1, "Read timed out", Request.HttpMethod.POST, new Date(), dummyReq);

        when(gestoPagoTxClient.sendTxConApiKey(anyString(), anyString(), any())).thenThrow(timeoutEx);

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idServicio(1)
                .idProducto(2)
                .upc("TIMEOUT001")
                .build();

        assertThrows(GestoPagoTimeoutException.class, () -> integrationService.enviarTransaccion(payload));
    }
}

