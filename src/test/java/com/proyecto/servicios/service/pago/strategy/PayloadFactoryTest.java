package com.proyecto.servicios.service.pago.strategy;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.enums.ModoCaptura;
import com.proyecto.servicios.exception.ProductoNoSoportadoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.pago.CalculoOperacionPago;
import com.proyecto.servicios.model.pago.RealizarPagoRequest;
import com.proyecto.servicios.service.CatalogoClasificadorComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PayloadFactoryTest {

    private PayloadFactory payloadFactory;
    private CatalogoClasificadorComponent clasificador;

    @BeforeEach
    void setUp() {
        payloadFactory = new PayloadFactory(List.of(
                new RecargaPayloadStrategy(),
                new PagoReferenciaPayloadStrategy(),
                new PagoCodigoBarrasPayloadStrategy(),
                new PrepagoPinPayloadStrategy()
        ));
        clasificador = new CatalogoClasificadorComponent();
    }

    @Test
    @DisplayName("Clasificador calcula correctamente ModoCaptura para todos los tipos de producto")
    void clasificadorCalculaModoCapturaCorrectamente() {
        assertEquals(ModoCaptura.RECARGA,
                CatalogoClasificadorComponent.calcularModoCaptura(1, 1, "a"));
        assertEquals(ModoCaptura.PAGO_REFERENCIA,
                CatalogoClasificadorComponent.calcularModoCaptura(2, 15, "b"));
        assertEquals(ModoCaptura.PAGO_CODIGO_BARRAS,
                CatalogoClasificadorComponent.calcularModoCaptura(2, 15, "c"));
        assertEquals(ModoCaptura.PREPAGO_PIN,
                CatalogoClasificadorComponent.calcularModoCaptura(1, 10, "a"));
        assertEquals(ModoCaptura.PREPAGO_PIN,
                CatalogoClasificadorComponent.calcularModoCaptura(1, 11, "a"));
        assertEquals(ModoCaptura.NO_SOPORTADO,
                CatalogoClasificadorComponent.calcularModoCaptura(4, 15, "b"));
        assertEquals(ModoCaptura.NO_SOPORTADO,
                CatalogoClasificadorComponent.calcularModoCaptura(30, 1, "a"));
    }

    @Test
    @DisplayName("Estrategia RECARGA: valida que monto coincida con el precio del catálogo y valida teléfono de 10 dígitos")
    void estrategiaRecargaFuncionaCorrectamente() {
        GestoPagoProducto prod = new GestoPagoProducto();
        prod.setIdServicio(1);
        prod.setIdProducto(10);
        prod.setTipoFront(1);
        prod.setPrecio("50.00");
        prod.setModoCaptura(ModoCaptura.RECARGA);

        RealizarPagoRequest req = RealizarPagoRequest.builder()
                .numeroCuenta("1234567890")
                .idProducto(10)
                .telefono("5512345678")
                .monto(new BigDecimal("50.00"))
                .pin("0042")
                .build();

        PagoPayloadStrategy strategy = payloadFactory.obtenerEstrategia(ModoCaptura.RECARGA);
        CalculoOperacionPago calculo = strategy.construirYValidar(prod, req, "UPC1234567", "20261026183000", "1");

        assertEquals(new BigDecimal("50.00"), calculo.getMonto());
        assertEquals(new BigDecimal("0.00"), calculo.getComision());
        assertEquals(new BigDecimal("50.00"), calculo.getTotalCobrado());
        assertEquals("5512345678", calculo.getPayload().getTelefono());
        assertNull(calculo.getPayload().getReferencia());

        // Monto distinto al del catálogo debe lanzar error
        req.setMonto(new BigDecimal("999.00"));
        assertThrows(ValidacionException.class,
                () -> strategy.construirYValidar(prod, req, "UPC1234567", "20261026183000", "1"));

        // Teléfono inválido de 9 dígitos debe fallar
        req.setMonto(new BigDecimal("50.00"));
        req.setTelefono("551234567");
        assertThrows(ValidacionException.class,
                () -> strategy.construirYValidar(prod, req, "UPC1234567", "20261026183000", "1"));
    }

    @Test
    @DisplayName("Estrategia PAGO_REFERENCIA: usa teléfono fijo 1111111111 y suma monto + comisión del catálogo")
    void estrategiaPagoReferenciaFuncionaCorrectamente() {
        GestoPagoProducto prod = new GestoPagoProducto();
        prod.setIdServicio(20);
        prod.setIdProducto(200);
        prod.setTipoFront(2);
        prod.setTipoReferencia("b");
        prod.setPrecio("12.50"); // comisión
        prod.setModoCaptura(ModoCaptura.PAGO_REFERENCIA);

        RealizarPagoRequest req = RealizarPagoRequest.builder()
                .numeroCuenta("1234567890")
                .idProducto(200)
                .referencia("CFE987654321")
                .monto(new BigDecimal("350.00"))
                .pin("0042")
                .build();

        PagoPayloadStrategy strategy = payloadFactory.obtenerEstrategia(ModoCaptura.PAGO_REFERENCIA);
        CalculoOperacionPago calculo = strategy.construirYValidar(prod, req, "UPC7654321", "20261026183000", "1");

        assertEquals("1111111111", calculo.getPayload().getTelefono());
        assertEquals("CFE987654321", calculo.getPayload().getReferencia());
        assertEquals(new BigDecimal("350.00"), calculo.getMonto());
        assertEquals(new BigDecimal("12.50"), calculo.getComision());
        assertEquals(new BigDecimal("362.50"), calculo.getTotalCobrado());
        assertEquals("350", calculo.getPayload().getMontoPago());
    }

    @Test
    @DisplayName("Estrategia PAGO_CODIGO_BARRAS: valida código de barras, usa teléfono fijo 1111111111 y suma comisión")
    void estrategiaPagoCodigoBarrasFuncionaCorrectamente() {
        GestoPagoProducto prod = new GestoPagoProducto();
        prod.setIdServicio(30);
        prod.setIdProducto(300);
        prod.setTipoFront(2);
        prod.setTipoReferencia("c");
        prod.setPrecio("8.00");
        prod.setModoCaptura(ModoCaptura.PAGO_CODIGO_BARRAS);

        RealizarPagoRequest req = RealizarPagoRequest.builder()
                .numeroCuenta("1234567890")
                .idProducto(300)
                .referencia("750105531180499210")
                .monto(new BigDecimal("120.00"))
                .pin("0042")
                .build();

        PagoPayloadStrategy strategy = payloadFactory.obtenerEstrategia(ModoCaptura.PAGO_CODIGO_BARRAS);
        CalculoOperacionPago calculo = strategy.construirYValidar(prod, req, "UPCBAR1234", "20261026183000", "1");

        assertEquals("1111111111", calculo.getPayload().getTelefono());
        assertEquals("750105531180499210", calculo.getPayload().getReferencia());
        assertEquals(new BigDecimal("120.00"), calculo.getMonto());
        assertEquals(new BigDecimal("8.00"), calculo.getComision());
        assertEquals(new BigDecimal("128.00"), calculo.getTotalCobrado());
    }

    @Test
    @DisplayName("Estrategia PREPAGO_PIN: usa teléfono fijo #1111111111 y valida que monto coincida con precio del catálogo")
    void estrategiaPrepagoPinFuncionaCorrectamente() {
        GestoPagoProducto prod = new GestoPagoProducto();
        prod.setIdServicio(40);
        prod.setIdProducto(400);
        prod.setIdCatTipoServicio(10);
        prod.setTipoFront(1);
        prod.setPrecio("199.00");
        prod.setModoCaptura(ModoCaptura.PREPAGO_PIN);

        RealizarPagoRequest req = RealizarPagoRequest.builder()
                .numeroCuenta("1234567890")
                .idProducto(400)
                .monto(new BigDecimal("199.00"))
                .pin("0042")
                .build();

        PagoPayloadStrategy strategy = payloadFactory.obtenerEstrategia(ModoCaptura.PREPAGO_PIN);
        CalculoOperacionPago calculo = strategy.construirYValidar(prod, req, "UPCPIN9999", "20261026183000", "1");

        assertEquals("#1111111111", calculo.getPayload().getTelefono());
        assertEquals(new BigDecimal("199.00"), calculo.getTotalCobrado());
        assertEquals(new BigDecimal("0.00"), calculo.getComision());

        // Monto distinto al del catálogo debe fallar
        req.setMonto(new BigDecimal("100.00"));
        assertThrows(ValidacionException.class,
                () -> strategy.construirYValidar(prod, req, "UPCPIN9999", "20261026183000", "1"));
    }

    @Test
    @DisplayName("PayloadFactory rechaza modo NO_SOPORTADO")
    void payloadFactoryRechazaNoSoportado() {
        assertThrows(ProductoNoSoportadoException.class,
                () -> payloadFactory.obtenerEstrategia(ModoCaptura.NO_SOPORTADO));
    }
}
