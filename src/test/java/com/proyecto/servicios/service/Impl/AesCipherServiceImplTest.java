package com.proyecto.servicios.service.Impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.proyecto.servicios.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class AesCipherServiceImplTest {

    private static final String TEST_SECRET_KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String TEST_IV = "p5ldmBPdd/9pjC0bDC/nSg==";

    private AesCipherServiceImpl aesCipherService;

    @BeforeEach
    void setUp() {
        aesCipherService = new AesCipherServiceImpl(TEST_SECRET_KEY, TEST_IV);
    }

    @Test
    @DisplayName("Debe cifrar y descifrar correctamente un payload JSON con AES-256-CBC PKCS5Padding")
    void debeCifrarYDescifrarCorrectamente() {
        String jsonPayload = "{\"idServicio\":56,\"idProducto\":185,\"telefono\":\"5512345678\",\"horaLocal\":\"20261026183000\",\"montoPago\":\"100.00\",\"upc\":\"A1B2C3D4E5\",\"unidad\":\"1\"}";

        String cifradoBase64 = aesCipherService.encrypt(jsonPayload);

        assertNotNull(cifradoBase64);
        assertFalse(cifradoBase64.isBlank());
        assertNotEquals(jsonPayload, cifradoBase64);

        String descifrado = aesCipherService.decrypt(cifradoBase64);
        assertEquals(jsonPayload, descifrado);
    }

    @Test
    @DisplayName("Debe lanzar BusinessException cuando el texto cifrado es inválido sin exponer llaves en logs")
    void debeFallarConTextoCifradoCorruptoSinExponerLlavesEnLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(AesCipherServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        String payloadSensible = "TextoCorruptoNoBase64$$$";
        assertThrows(BusinessException.class, () -> aesCipherService.decrypt(payloadSensible));

        for (ILoggingEvent event : listAppender.list) {
            String msg = event.getFormattedMessage();
            assertFalse(msg.contains(TEST_SECRET_KEY), "El log no debe contener SECRET_KEY");
            assertFalse(msg.contains(TEST_IV), "El log no debe contener IV");
            assertFalse(msg.contains(payloadSensible), "El log no debe contener el payload sensible");
        }

        logger.detachAppender(listAppender);
    }
}

