package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.enums.ErrorCode;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.exception.GestoPagoAuthException;
import com.proyecto.servicios.exception.GestoPagoComunicacionException;
import com.proyecto.servicios.exception.GestoPagoProveedorRechazoException;
import com.proyecto.servicios.exception.GestoPagoRespuestaInvalidaException;
import com.proyecto.servicios.exception.GestoPagoTimeoutException;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxXmlResponse;
import com.proyecto.servicios.service.AesCipherService;
import com.proyecto.servicios.service.GestoPagoIntegrationService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import feign.RetryableException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.StringReader;
import java.net.SocketTimeoutException;
import java.util.regex.Pattern;

@Service
@Slf4j
public class GestoPagoIntegrationServiceImpl implements GestoPagoIntegrationService {

    private static final Pattern AMPERSAND_NO_ESCAPADO =
            Pattern.compile("&(?!amp;|lt;|gt;|quot;|apos;|#\\d+;|#x[0-9a-fA-F]+;)");

    private static final Pattern MENSAJE_PLANO_SIN_HIJOS =
            Pattern.compile("<MENSAJE>\\s*([^<]*?)\\s*</MENSAJE>", Pattern.DOTALL);

    private final GestoPagoTxClient gestoPagoTxClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final AesCipherService aesCipherService;
    private final ObjectMapper objectMapper;

    @Value("${gestopago.tx.api-key:}")
    private String apiKey;

    public GestoPagoIntegrationServiceImpl(
            GestoPagoTxClient gestoPagoTxClient,
            GestoPagoTokenService gestoPagoTokenService,
            AesCipherService aesCipherService,
            ObjectMapper objectMapper) {
        this.gestoPagoTxClient = gestoPagoTxClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.aesCipherService = aesCipherService;
        this.objectMapper = objectMapper;
    }

    @Override
    public GestoPagoSendTxXmlResponse enviarTransaccion(GestoPagoSendTxPayload payload) {
        if (payload == null) {
            throw new BusinessException(ErrorCode.VALIDACION_ERROR, "El payload de transacción no puede ser nulo");
        }

        log.info("Iniciando invocación externa sendTx a GestoPago [upc={}, idServicio={}, idProducto={}]",
                payload.getUpc(), payload.getIdServicio(), payload.getIdProducto());

        MultiValueMap<String, String> formParams = construirParametrosCifradosForm(payload);
        String headerApiKey = (apiKey != null && !apiKey.isBlank()) ? apiKey.trim() : null;
        String token = gestoPagoTokenService.obtenerTokenValido();

        String xmlRespuesta;
        try {
            xmlRespuesta = ejecutarConReintentoAuthUnico(token, headerApiKey, formParams, payload.getUpc());
        } finally {
            log.info("Finalizó invocación externa sendTx a GestoPago [upc={}]", payload.getUpc());
        }

        GestoPagoSendTxXmlResponse response = parsearRespuestaXml(xmlRespuesta, payload.getUpc());
        String codigo = response.obtenerCodigoEfectivo();

        if (codigo == null || codigo.isBlank()) {
            log.error("TX-GP-005: La respuesta XML de GestoPago no contiene CODIGO [upc={}]", payload.getUpc());
            throw new GestoPagoRespuestaInvalidaException("El proveedor devolvió una respuesta sin código de estatus");
        }

        if (!"01".equals(codigo)) {
            String textoError = response.obtenerTextoEfectivo();
            if ("82".equals(codigo)) {
                log.error("TX-GP-002: GestoPago devolvió código 82 (timeout en proveedor) [upc={}]", payload.getUpc());
                throw new GestoPagoTimeoutException(textoError != null
                        ? textoError
                        : "Tiempo de espera alcanzado con el proveedor; la operación quedó en revisión");
            }
            log.warn("TX-GP-006: Proveedor rechazó la transacción [upc={}, codigo={}]", payload.getUpc(), codigo);
            throw new GestoPagoProveedorRechazoException(codigo, textoError);
        }

        log.info("Respuesta exitosa de GestoPago sendTx [upc={}, codigo=01, idTx={}]",
                payload.getUpc(), response.getIdTx());
        return response;
    }

    private String ejecutarConReintentoAuthUnico(
            String tokenInicial,
            String headerApiKey,
            MultiValueMap<String, String> formParams,
            String upc) {
        try {
            return invocarSendTx(tokenInicial, headerApiKey, formParams);
        } catch (FeignException e) {
            if (e.status() == 403 || e.status() == 401) {
                log.warn("TX-GP-001: GestoPago devolvió HTTP {} [upc={}]. Renovando token y reintentando una única vez",
                        e.status(), upc);
                String nuevoToken = gestoPagoTokenService.renovarYObtenerToken(tokenInicial);
                try {
                    return invocarSendTx(nuevoToken, headerApiKey, formParams);
                } catch (FeignException retryEx) {
                    return manejarFeignExceptionSinReintento(retryEx, upc);
                } catch (Exception retryEx) {
                    return manejarExcepcionGeneral(retryEx, upc);
                }
            }
            return manejarFeignExceptionSinReintento(e, upc);
        } catch (Exception e) {
            return manejarExcepcionGeneral(e, upc);
        }
    }

    private String invocarSendTx(String token, String headerApiKey, MultiValueMap<String, String> formParams) {
        String bearer = "Bearer " + token;
        if (headerApiKey != null && !headerApiKey.isBlank()) {
            return gestoPagoTxClient.sendTxConApiKey(bearer, headerApiKey, formParams);
        }
        return gestoPagoTxClient.sendTx(bearer, formParams);
    }

    private String manejarFeignExceptionSinReintento(FeignException e, String upc) {
        if (e instanceof RetryableException || esCausaTimeout(e)) {
            log.error("TX-GP-002: Timeout al invocar sendTx en GestoPago [upc={}]", upc);
            throw new GestoPagoTimeoutException("La operación con el proveedor agotó el tiempo de espera y quedó en revisión");
        }
        if (e.status() == 401 || e.status() == 403) {
            log.error("TX-GP-003: Error de autenticación persistente con GestoPago HTTP {} [upc={}]", e.status(), upc);
            throw new GestoPagoAuthException("El proveedor rechazó la autenticación de la transacción");
        }
        if (e.status() < 0) {
            log.error("TX-GP-002: Respuesta ambigua o desconexión de red en sendTx [upc={}]", upc);
            throw new GestoPagoTimeoutException("Error de red durante la transacción; la operación quedó en revisión");
        }
        log.error("TX-GP-004: Error HTTP {} al invocar sendTx en GestoPago [upc={}]", e.status(), upc);
        throw new GestoPagoComunicacionException("El proveedor respondió con un estado HTTP de error (" + e.status() + ")");
    }

    private String manejarExcepcionGeneral(Exception e, String upc) {
        if (e instanceof GestoPagoTimeoutException
                || e instanceof GestoPagoAuthException
                || e instanceof GestoPagoComunicacionException) {
            throw (RuntimeException) e;
        }
        if (esCausaTimeout(e)) {
            log.error("TX-GP-002: Timeout de lectura/conexión en sendTx [upc={}]", upc);
            throw new GestoPagoTimeoutException("Tiempo de espera agotado con el proveedor; operación en revisión");
        }
        log.error("TX-GP-004: Error de comunicación al invocar sendTx [upc={}, tipo={}]",
                upc, e.getClass().getSimpleName());
        throw new GestoPagoComunicacionException("Ocurrió un fallo de comunicación con el proveedor de servicios");
    }

    private boolean esCausaTimeout(Throwable t) {
        Throwable actual = t;
        while (actual != null) {
            if (actual instanceof SocketTimeoutException
                    || (actual.getMessage() != null && actual.getMessage().toLowerCase().contains("timed out"))
                    || (actual.getMessage() != null && actual.getMessage().toLowerCase().contains("timeout"))) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    private MultiValueMap<String, String> construirParametrosCifradosForm(GestoPagoSendTxPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            String signedBase64 = aesCipherService.encrypt(json);
            String random = String.valueOf(System.currentTimeMillis());

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("random", random);
            params.add("signed", signedBase64);
            return params;
        } catch (JsonProcessingException e) {
            log.error("TX-GP-007: Error al serializar payload para sendTx [upc={}]", payload.getUpc());
            throw new BusinessException(ErrorCode.GESTOPAGO_CIFRADO_ERROR, "Error al construir la petición hacia el proveedor");
        }
    }

    private GestoPagoSendTxXmlResponse parsearRespuestaXml(String xml, String upc) {
        if (xml == null || xml.isBlank()) {
            log.error("TX-GP-005: GestoPago devolvió respuesta vacía en sendTx [upc={}]", upc);
            throw new GestoPagoRespuestaInvalidaException("El proveedor devolvió una respuesta vacía");
        }

        String xmlSanitizado = sanitizarXmlProveedor(xml);

        try {
            JAXBContext context = JAXBContext.newInstance(GestoPagoSendTxXmlResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (GestoPagoSendTxXmlResponse) unmarshaller.unmarshal(new StringReader(xmlSanitizado));
        } catch (JAXBException e) {
            String detalleError = e.getLinkedException() != null ? e.getLinkedException().getMessage() : e.getMessage();
            log.error("TX-GP-005: Error al parsear XML de sendTx [upc={}, causa={}]", upc, detalleError);
            throw new GestoPagoRespuestaInvalidaException("El proveedor devolvió un XML con formato inválido");
        }
    }

    private String sanitizarXmlProveedor(String rawXml) {
        String limpio = rawXml.replace("\uFEFF", "").trim();
        int primerMenorQue = limpio.indexOf('<');
        if (primerMenorQue > 0) {
            limpio = limpio.substring(primerMenorQue);
        }
        // Normalizar raíz <AUTORIZACION> que GestoPago devuelve en rechazos/respuestas de carrier hacia <RESPONSE>
        limpio = limpio.replace("<AUTORIZACION>", "<RESPONSE>")
                .replace("</AUTORIZACION>", "</RESPONSE>");
        // Si <MENSAJE> contiene texto plano directo (sin etiquetas hijas), mapearlo a <TEXTO>
        limpio = MENSAJE_PLANO_SIN_HIJOS.matcher(limpio).replaceAll("<TEXTO>$1</TEXTO>");
        return AMPERSAND_NO_ESCAPADO.matcher(limpio).replaceAll("&amp;");
    }
}

