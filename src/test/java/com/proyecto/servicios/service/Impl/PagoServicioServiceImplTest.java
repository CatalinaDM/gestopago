package com.proyecto.servicios.service.Impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.entity.pago.MovimientoCuenta;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.enums.ModoCaptura;
import com.proyecto.servicios.enums.TipoMovimientoCuenta;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.GestoPagoProveedorRechazoException;
import com.proyecto.servicios.exception.GestoPagoTimeoutException;
import com.proyecto.servicios.exception.NipInvalidoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.mapper.TransaccionPagoMapper;
import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxXmlResponse;
import com.proyecto.servicios.model.pago.ComprobantePagoResponse;
import com.proyecto.servicios.model.pago.RealizarPagoRequest;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.pago.MovimientoCuentaRepository;
import com.proyecto.servicios.repositorys.pago.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.usuario.UsuarioRepository;
import com.proyecto.servicios.service.AutorizacionTransaccionService;
import com.proyecto.servicios.service.CatalogoConsultaService;
import com.proyecto.servicios.service.GestoPagoIntegrationService;
import com.proyecto.servicios.service.PagoTransaccionHelper;
import com.proyecto.servicios.service.pago.strategy.PagoCodigoBarrasPayloadStrategy;
import com.proyecto.servicios.service.pago.strategy.PagoReferenciaPayloadStrategy;
import com.proyecto.servicios.service.pago.strategy.PayloadFactory;
import com.proyecto.servicios.service.pago.strategy.PrepagoPinPayloadStrategy;
import com.proyecto.servicios.service.pago.strategy.RecargaPayloadStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServicioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TransaccionPagoRepository transaccionPagoRepository;

    @Mock
    private MovimientoCuentaRepository movimientoCuentaRepository;

    @Mock
    private CatalogoConsultaService catalogoConsultaService;

    @Mock
    private AutorizacionTransaccionService autorizacionTransaccionService;

    @Mock
    private GestoPagoIntegrationService gestoPagoIntegrationService;

    private PagoServicioServiceImpl pagoServicioService;

    @BeforeEach
    void setUp() {
        PayloadFactory payloadFactory = new PayloadFactory(List.of(
                new RecargaPayloadStrategy(),
                new PagoReferenciaPayloadStrategy(),
                new PagoCodigoBarrasPayloadStrategy(),
                new PrepagoPinPayloadStrategy()
        ));
        PagoTransaccionHelper helper = new PagoTransaccionHelper(
                cuentaRepository,
                transaccionPagoRepository,
                movimientoCuentaRepository
        );
        TransaccionPagoMapper mapper = Mappers.getMapper(TransaccionPagoMapper.class);

        pagoServicioService = new PagoServicioServiceImpl(
                usuarioRepository,
                cuentaRepository,
                transaccionPagoRepository,
                catalogoConsultaService,
                autorizacionTransaccionService,
                payloadFactory,
                helper,
                gestoPagoIntegrationService,
                mapper
        );
    }

    @Test
    @DisplayName("Pago exitoso: descuenta monto + comisión, confirma EXITOSA, devuelve comprobante y no registra NIP en logs")
    void realizarPago_Exitoso() {
        Logger loggerRoot = (Logger) LoggerFactory.getLogger("com.proyecto.servicios");
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        loggerRoot.addAppender(listAppender);

        Cliente cliente = crearClienteActivo(10);
        Usuario usuario = crearUsuarioActivo(1, cliente);
        Cuenta cuenta = crearCuenta(100, "1234567890", cliente, new BigDecimal("500.00"), true);
        GestoPagoProducto producto = crearProductoServicio(56, 185, "15.00");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuenta));
        when(catalogoConsultaService.obtenerProductoSoportadoPorId(185)).thenReturn(producto);
        when(transaccionPagoRepository.existsByUpc(anyString())).thenReturn(false);
        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(inv -> {
            TransaccionPago tx = inv.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(77);
            }
            return tx;
        });

        TransaccionPago txGuardada = new TransaccionPago();
        txGuardada.setId(77);
        txGuardada.setCuenta(cuenta);
        txGuardada.setIdProducto(185);
        txGuardada.setIdServicio(56);
        txGuardada.setUpc("UPC1234567");
        txGuardada.setMonto(new BigDecimal("200.00"));
        txGuardada.setComision(new BigDecimal("15.00"));
        txGuardada.setTotalCobrado(new BigDecimal("215.00"));
        txGuardada.setEstado(EstadoTransaccion.PENDIENTE);
        when(transaccionPagoRepository.findById(77)).thenReturn(Optional.of(txGuardada));

        GestoPagoSendTxXmlResponse xmlResponse = new GestoPagoSendTxXmlResponse();
        xmlResponse.setCodigo("01");
        xmlResponse.setTexto("Pago aplicado");
        xmlResponse.setIdTx("GP-TX-900");
        xmlResponse.setNumAutorizacion("AUT-777");
        xmlResponse.setLeyenda("Gracias por su pago");
        when(gestoPagoIntegrationService.enviarTransaccion(any())).thenReturn(xmlResponse);

        String nipSecreto = "0482";
        RealizarPagoRequest req = RealizarPagoRequest.builder()
                .numeroCuenta("1234567890")
                .idProducto(185)
                .referencia("REF998877")
                .monto(new BigDecimal("200.00"))
                .pin(nipSecreto)
                .build();
        GenericRequest<RealizarPagoRequest> genericReq = new GenericRequest<>();
        genericReq.setDatos(req);

        ComprobantePagoResponse comprobante = pagoServicioService.realizarPago(genericReq, 1, 10);

        assertNotNull(comprobante);
        assertEquals(EstadoTransaccion.EXITOSA, comprobante.getEstado());
        assertEquals(new BigDecimal("200.00"), comprobante.getMonto());
        assertEquals(new BigDecimal("15.00"), comprobante.getComision());
        assertEquals(new BigDecimal("215.00"), comprobante.getTotalCobrado());
        assertEquals(new BigDecimal("285.00"), cuenta.getSaldo());
        assertEquals("GP-TX-900", comprobante.getIdTxProveedor());
        assertEquals("AUT-777", comprobante.getNumAutorizacion());

        for (ILoggingEvent event : listAppender.list) {
            assertFalse(event.getFormattedMessage().contains(nipSecreto),
                    "Ningún log debe contener el NIP transaccional");
        }
        loggerRoot.detachAppender(listAppender);
    }

    @Test
    @DisplayName("Saldo insuficiente: lanza SaldoInsuficienteException y no invoca a GestoPago")
    void realizarPago_SaldoInsuficiente() {
        Cliente cliente = crearClienteActivo(10);
        Usuario usuario = crearUsuarioActivo(1, cliente);
        Cuenta cuenta = crearCuenta(100, "1234567890", cliente, new BigDecimal("50.00"), true);
        GestoPagoProducto producto = crearProductoServicio(56, 185, "15.00");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuenta));
        when(catalogoConsultaService.obtenerProductoSoportadoPorId(185)).thenReturn(producto);

        RealizarPagoRequest req = RealizarPagoRequest.builder()
                .numeroCuenta("1234567890")
                .idProducto(185)
                .referencia("REF998877")
                .monto(new BigDecimal("100.00")) // total 115 > 50
                .pin("0042")
                .build();
        GenericRequest<RealizarPagoRequest> genericReq = new GenericRequest<>();
        genericReq.setDatos(req);

        assertThrows(SaldoInsuficienteException.class,
                () -> pagoServicioService.realizarPago(genericReq, 1, 10));

        assertEquals(new BigDecimal("50.00"), cuenta.getSaldo());
        verify(gestoPagoIntegrationService, never()).enviarTransaccion(any());
    }

    @Test
    @DisplayName("Cuenta ajena o inactiva: rechaza antes de tocar saldo ni llamar a GestoPago")
    void realizarPago_CuentaAjenaOInactiva() {
        Cliente clienteAutenticado = crearClienteActivo(10);
        Cliente otroCliente = crearClienteActivo(99);
        Usuario usuario = crearUsuarioActivo(1, clienteAutenticado);

        Cuenta cuentaAjena = crearCuenta(101, "9999999999", otroCliente, new BigDecimal("1000.00"), true);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("9999999999")).thenReturn(Optional.of(cuentaAjena));

        RealizarPagoRequest reqAjena = RealizarPagoRequest.builder()
                .numeroCuenta("9999999999")
                .idProducto(185)
                .referencia("REF123")
                .monto(new BigDecimal("100.00"))
                .pin("0042")
                .build();
        GenericRequest<RealizarPagoRequest> genericAjena = new GenericRequest<>();
        genericAjena.setDatos(reqAjena);

        assertThrows(AccesoDenegadoException.class,
                () -> pagoServicioService.realizarPago(genericAjena, 1, 10));

        // Cuenta propia pero inactiva
        Cuenta cuentaInactiva = crearCuenta(102, "1234567890", clienteAutenticado, new BigDecimal("1000.00"), false);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaInactiva));

        RealizarPagoRequest reqInactiva = RealizarPagoRequest.builder()
                .numeroCuenta("1234567890")
                .idProducto(185)
                .referencia("REF123")
                .monto(new BigDecimal("100.00"))
                .pin("0042")
                .build();
        GenericRequest<RealizarPagoRequest> genericInactiva = new GenericRequest<>();
        genericInactiva.setDatos(reqInactiva);

        assertThrows(CuentaInactivaException.class,
                () -> pagoServicioService.realizarPago(genericInactiva, 1, 10));

        verify(gestoPagoIntegrationService, never()).enviarTransaccion(any());
    }

    @Test
    @DisplayName("NIP incorrecto: rechaza la operación antes de reservar saldo")
    void realizarPago_NipIncorrectoNoTocaSaldo() {
        Cliente cliente = crearClienteActivo(10);
        Usuario usuario = crearUsuarioActivo(1, cliente);
        Cuenta cuenta = crearCuenta(100, "1234567890", cliente, new BigDecimal("500.00"), true);
        GestoPagoProducto producto = crearProductoServicio(56, 185, "15.00");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(catalogoConsultaService.obtenerProductoSoportadoPorId(185)).thenReturn(producto);
        doThrow(new NipInvalidoException()).when(autorizacionTransaccionService).validarAutorizacion(1, "9999");

        RealizarPagoRequest req = RealizarPagoRequest.builder()
                .numeroCuenta("1234567890")
                .idProducto(185)
                .referencia("REF998877")
                .monto(new BigDecimal("100.00"))
                .pin("9999")
                .build();
        GenericRequest<RealizarPagoRequest> genericReq = new GenericRequest<>();
        genericReq.setDatos(req);

        assertThrows(NipInvalidoException.class,
                () -> pagoServicioService.realizarPago(genericReq, 1, 10));

        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
        verify(cuentaRepository, never()).findByNumeroCuentaForUpdate(anyString());
        verify(gestoPagoIntegrationService, never()).enviarTransaccion(any());
    }

    @Test
    @DisplayName("Error del proveedor (CODIGO != 01): marca FALLIDA, restaura saldo y registra movimiento inverso REEMBOLSO_PAGO")
    void realizarPago_ErrorProveedorEjecutaReembolso() {
        Cliente cliente = crearClienteActivo(10);
        Usuario usuario = crearUsuarioActivo(1, cliente);
        Cuenta cuenta = crearCuenta(100, "1234567890", cliente, new BigDecimal("500.00"), true);
        GestoPagoProducto producto = crearProductoServicio(56, 185, "10.00");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuenta));
        when(catalogoConsultaService.obtenerProductoSoportadoPorId(185)).thenReturn(producto);

        TransaccionPago[] txHolder = new TransaccionPago[1];
        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(inv -> {
            TransaccionPago tx = inv.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(88);
            }
            txHolder[0] = tx;
            return tx;
        });
        when(transaccionPagoRepository.findById(88)).thenAnswer(inv -> Optional.ofNullable(txHolder[0]));

        when(gestoPagoIntegrationService.enviarTransaccion(any()))
                .thenThrow(new GestoPagoProveedorRechazoException("08", "Referencia vencida"));

        RealizarPagoRequest req = RealizarPagoRequest.builder()
                .numeroCuenta("1234567890")
                .idProducto(185)
                .referencia("REF998877")
                .monto(new BigDecimal("100.00"))
                .pin("0042")
                .build();
        GenericRequest<RealizarPagoRequest> genericReq = new GenericRequest<>();
        genericReq.setDatos(req);

        assertThrows(GestoPagoProveedorRechazoException.class,
                () -> pagoServicioService.realizarPago(genericReq, 1, 10));

        // El saldo se descontó (500 -> 390) y luego se reembolsó (390 -> 500)
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());
        assertEquals(EstadoTransaccion.FALLIDA, txHolder[0].getEstado());
        assertEquals("08", txHolder[0].getCodigoRespuesta());

        ArgumentCaptor<MovimientoCuenta> movCaptor = ArgumentCaptor.forClass(MovimientoCuenta.class);
        verify(movimientoCuentaRepository, times(2)).save(movCaptor.capture());
        List<MovimientoCuenta> movimientos = movCaptor.getAllValues();
        assertEquals(TipoMovimientoCuenta.CARGO_PAGO, movimientos.get(0).getTipoMovimiento());
        assertEquals(TipoMovimientoCuenta.REEMBOLSO_PAGO, movimientos.get(1).getTipoMovimiento());
    }

    @Test
    @DisplayName("Timeout del proveedor: marca EN_REVISION sin reembolso automático y sin reintentar sendTx")
    void realizarPago_TimeoutMarcaEnRevisionSinReembolsoAutomatico() {
        Cliente cliente = crearClienteActivo(10);
        Usuario usuario = crearUsuarioActivo(1, cliente);
        Cuenta cuenta = crearCuenta(100, "1234567890", cliente, new BigDecimal("500.00"), true);
        GestoPagoProducto producto = crearProductoServicio(56, 185, "10.00");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findByNumeroCuentaForUpdate("1234567890")).thenReturn(Optional.of(cuenta));
        when(catalogoConsultaService.obtenerProductoSoportadoPorId(185)).thenReturn(producto);

        TransaccionPago[] txHolder = new TransaccionPago[1];
        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(inv -> {
            TransaccionPago tx = inv.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(99);
            }
            txHolder[0] = tx;
            return tx;
        });
        when(transaccionPagoRepository.findById(99)).thenAnswer(inv -> Optional.ofNullable(txHolder[0]));

        when(gestoPagoIntegrationService.enviarTransaccion(any()))
                .thenThrow(new GestoPagoTimeoutException("Read timed out"));

        RealizarPagoRequest req = RealizarPagoRequest.builder()
                .numeroCuenta("1234567890")
                .idProducto(185)
                .referencia("REF998877")
                .monto(new BigDecimal("100.00"))
                .pin("0042")
                .build();
        GenericRequest<RealizarPagoRequest> genericReq = new GenericRequest<>();
        genericReq.setDatos(req);

        assertThrows(GestoPagoTimeoutException.class,
                () -> pagoServicioService.realizarPago(genericReq, 1, 10));

        // Queda EN_REVISION, el saldo permanece reservado (390.00) y solo existe 1 movimiento (CARGO_PAGO, sin reembolso automático)
        assertEquals(EstadoTransaccion.EN_REVISION, txHolder[0].getEstado());
        assertEquals(new BigDecimal("390.00"), cuenta.getSaldo());
        verify(movimientoCuentaRepository, times(1)).save(any(MovimientoCuenta.class));
        verify(gestoPagoIntegrationService, times(1)).enviarTransaccion(any());
    }

    private Cliente crearClienteActivo(Integer id) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setActivo(true);
        return cliente;
    }

    private Usuario crearUsuarioActivo(Integer id, Cliente cliente) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setCorreo("cliente@banco.com");
        usuario.setActivo(true);
        usuario.setCliente(cliente);
        return usuario;
    }

    private Cuenta crearCuenta(Integer id, String numeroCuenta, Cliente cliente, BigDecimal saldo, boolean activa) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(id);
        cuenta.setNumeroCuenta(numeroCuenta);
        cuenta.setCliente(cliente);
        cuenta.setSaldo(saldo);
        cuenta.setActivo(activa);
        return cuenta;
    }

    private GestoPagoProducto crearProductoServicio(Integer idServicio, Integer idProducto, String precioComision) {
        GestoPagoProducto prod = new GestoPagoProducto();
        prod.setIdServicio(idServicio);
        prod.setIdProducto(idProducto);
        prod.setIdCatTipoServicio(15);
        prod.setServicio("CFE");
        prod.setProducto("Pago CFE");
        prod.setTipoFront(2);
        prod.setTipoReferencia("b");
        prod.setPrecio(precioComision);
        prod.setModoCaptura(ModoCaptura.PAGO_REFERENCIA);
        return prod;
    }
}

