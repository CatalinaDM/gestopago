package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.enums.ErrorCode;
import com.proyecto.servicios.enums.ModoCaptura;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.GestoPagoProveedorRechazoException;
import com.proyecto.servicios.exception.GestoPagoTimeoutException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.mapper.TransaccionPagoMapper;
import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxXmlResponse;
import com.proyecto.servicios.model.pago.AutorizacionNipResponse;
import com.proyecto.servicios.model.pago.CalculoOperacionPago;
import com.proyecto.servicios.model.pago.ComprobantePagoResponse;
import com.proyecto.servicios.model.pago.RealizarPagoRequest;
import com.proyecto.servicios.model.pago.SolicitarNipTransaccionRequest;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.pago.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.usuario.UsuarioRepository;
import com.proyecto.servicios.service.AutorizacionTransaccionService;
import com.proyecto.servicios.service.CatalogoConsultaService;
import com.proyecto.servicios.service.GestoPagoIntegrationService;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.service.PagoTransaccionHelper;
import com.proyecto.servicios.service.pago.strategy.PagoPayloadStrategy;
import com.proyecto.servicios.service.pago.strategy.PagoReferenciaPayloadStrategy;
import com.proyecto.servicios.service.pago.strategy.PayloadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
public class PagoServicioServiceImpl implements PagoServicioService {

    private static final String CARACTERES_UPC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LONGITUD_UPC = 10;
    private static final ZoneId ZONA_MEXICO = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter FORMATO_HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final CatalogoConsultaService catalogoConsultaService;
    private final AutorizacionTransaccionService autorizacionTransaccionService;
    private final PayloadFactory payloadFactory;
    private final PagoTransaccionHelper pagoTransaccionHelper;
    private final GestoPagoIntegrationService gestoPagoIntegrationService;
    private final TransaccionPagoMapper transaccionPagoMapper;

    @Value("${gestopago.tx.unidad:083}")
    private String unidadConfigurada = "083";

    @Value("${gestopago.tx.validar-referencia-previa:false}")
    private boolean validarReferenciaPrevia = false;

    public PagoServicioServiceImpl(
            UsuarioRepository usuarioRepository,
            CuentaRepository cuentaRepository,
            TransaccionPagoRepository transaccionPagoRepository,
            CatalogoConsultaService catalogoConsultaService,
            AutorizacionTransaccionService autorizacionTransaccionService,
            PayloadFactory payloadFactory,
            PagoTransaccionHelper pagoTransaccionHelper,
            GestoPagoIntegrationService gestoPagoIntegrationService,
            TransaccionPagoMapper transaccionPagoMapper) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.transaccionPagoRepository = transaccionPagoRepository;
        this.catalogoConsultaService = catalogoConsultaService;
        this.autorizacionTransaccionService = autorizacionTransaccionService;
        this.payloadFactory = payloadFactory;
        this.pagoTransaccionHelper = pagoTransaccionHelper;
        this.gestoPagoIntegrationService = gestoPagoIntegrationService;
        this.transaccionPagoMapper = transaccionPagoMapper;
    }

    @Override
    public AutorizacionNipResponse solicitarNipParaPago(
            GenericRequest<SolicitarNipTransaccionRequest> genericRequest,
            Integer usuarioId,
            Integer clienteId) {

        if (genericRequest == null || genericRequest.getDatos() == null) {
            throw new ValidacionException("VALIDACION-001", "Los datos de la petición son obligatorios");
        }
        return autorizacionTransaccionService.emitirNipParaPago(
                usuarioId,
                clienteId,
                genericRequest.getDatos().getNumeroCuenta()
        );
    }

    @Override
    public ComprobantePagoResponse realizarPago(
            GenericRequest<RealizarPagoRequest> genericRequest,
            Integer usuarioId,
            Integer clienteId) {

        if (genericRequest == null || genericRequest.getDatos() == null) {
            throw new ValidacionException("VALIDACION-001", "Los datos de la petición de pago son obligatorios");
        }
        RealizarPagoRequest request = genericRequest.getDatos();

        // 1. Validar que JWT corresponda a un usuario y cliente activos
        validarUsuarioYClienteActivos(usuarioId, clienteId);

        // 2. Validar que numeroCuenta pertenezca al usuario autenticado y esté ACTIVA
        String numeroCuenta = request.getNumeroCuenta().trim();
        validarCuentaActivaYPropiedad(numeroCuenta, clienteId);

        // 3. Validar que el producto exista en el catálogo local y sea soportado
        GestoPagoProducto productoCatalogo = catalogoConsultaService.obtenerProductoSoportadoPorId(request.getIdProducto());

        // 4. Validar formato de teléfono/referencia/monto según el modo de captura (Strategy/Factory)
        String upc = generarUpcUnico();
        String horaLocal = ZonedDateTime.now(ZONA_MEXICO).format(FORMATO_HORA_LOCAL);
        String unidadEfectiva = obtenerUnidadNormalizada();
        PagoPayloadStrategy estrategia = payloadFactory.obtenerEstrategia(productoCatalogo.getModoCaptura());
        CalculoOperacionPago calculo = estrategia.construirYValidar(
                productoCatalogo,
                request,
                upc,
                horaLocal,
                unidadEfectiva
        );

        // 5. Validar PIN transaccional antes de tocar el saldo
        autorizacionTransaccionService.validarAutorizacion(usuarioId, request.getPin());

        // E (Opcional, detrás de flag): Validación previa de referencia con producto tipoFront 4 si está disponible
        ejecutarValidacionPreviaReferenciaSiAplica(productoCatalogo, calculo, horaLocal);

        // 6 y Reserva (Primera transacción corta): Bloquea cuenta (PESSIMISTIC_WRITE), valida saldo >= monto+comision, descuenta y guarda PENDIENTE
        TransaccionPago transaccionPendiente = pagoTransaccionHelper.reservarSaldoYCrearPendiente(
                numeroCuenta,
                clienteId,
                productoCatalogo,
                calculo,
                upc
        );

        // Llamada a GestoPago FUERA de la transacción de base de datos
        GestoPagoSendTxXmlResponse xmlResponse;
        try {
            xmlResponse = gestoPagoIntegrationService.enviarTransaccion(calculo.getPayload());
        } catch (GestoPagoTimeoutException timeoutEx) {
            // Timeout o respuesta ambigua -> EN_REVISION sin reembolso automático ni reintento a ciegas
            pagoTransaccionHelper.marcarEnRevision(
                    transaccionPendiente.getId(),
                    timeoutEx.getMessage()
            );
            throw timeoutEx;
        } catch (GestoPagoProveedorRechazoException rechazoEx) {
            // Rechazo explícito del proveedor -> FALLIDA + reembolso (movimiento inverso)
            pagoTransaccionHelper.reversarTransaccionFallida(
                    transaccionPendiente.getId(),
                    numeroCuenta,
                    rechazoEx.getCodigoProveedor(),
                    rechazoEx.getMessage()
            );
            throw rechazoEx;
        } catch (RuntimeException errorClaroEx) {
            // Error claro antes de confirmar en el proveedor -> FALLIDA + reembolso
            pagoTransaccionHelper.reversarTransaccionFallida(
                    transaccionPendiente.getId(),
                    numeroCuenta,
                    "ERR",
                    errorClaroEx.getMessage()
            );
            throw errorClaroEx;
        }

        // Segunda transacción corta (CODIGO == "01"): Confirmar EXITOSA y guardar comprobante
        TransaccionPago transaccionConfirmada = pagoTransaccionHelper.confirmarTransaccionExitosa(
                transaccionPendiente.getId(),
                xmlResponse
        );

        String mensajeExito = xmlResponse.obtenerTextoEfectivo() != null
                ? xmlResponse.obtenerTextoEfectivo()
                : "Pago aplicado exitosamente";

        return transaccionPagoMapper.toComprobanteResponse(
                transaccionConfirmada,
                productoCatalogo,
                numeroCuenta,
                transaccionPendiente.getCuenta().getSaldo(),
                mensajeExito
        );
    }

    private void validarUsuarioYClienteActivos(Integer usuarioId, Integer clienteId) {
        if (usuarioId == null || clienteId == null) {
            throw new AccesoDenegadoException("AUTH-003", "Se requiere una sesión de cliente válida para realizar pagos");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(String.valueOf(usuarioId)));

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new UsuarioInactivoException(usuario.getCorreo());
        }

        if (usuario.getCliente() == null || !clienteId.equals(usuario.getCliente().getId())) {
            throw new AccesoDenegadoException("AUTH-004", "El usuario no corresponde al cliente de la sesión");
        }

        if (Boolean.FALSE.equals(usuario.getCliente().getActivo())) {
            throw new AccesoDenegadoException("CLIENTE-INACTIVO", ErrorCode.CLIENTE_INACTIVO.getMessage());
        }
    }

    private void validarCuentaActivaYPropiedad(String numeroCuenta, Integer clienteId) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));

        if (cuenta.getCliente() == null || !clienteId.equals(cuenta.getCliente().getId())) {
            log.warn("AUTH-004: Intento de pago desde cuenta ajena {} por clienteId {}", numeroCuenta, clienteId);
            throw new AccesoDenegadoException("AUTH-004",
                    "Acceso denegado: No puede realizar pagos desde una cuenta que no le pertenece");
        }

        if (Boolean.FALSE.equals(cuenta.getActivo())) {
            throw new CuentaInactivaException(numeroCuenta);
        }
    }

    private void ejecutarValidacionPreviaReferenciaSiAplica(
            GestoPagoProducto productoCatalogo,
            CalculoOperacionPago calculo,
            String horaLocal) {

        if (!validarReferenciaPrevia) {
            return;
        }
        ModoCaptura modo = productoCatalogo.getModoCaptura();
        if (modo != ModoCaptura.PAGO_REFERENCIA && modo != ModoCaptura.PAGO_CODIGO_BARRAS) {
            return;
        }

        Optional<GestoPagoProducto> prodVerificacionOpt = catalogoConsultaService
                .buscarProductoVerificacionPorServicio(productoCatalogo.getIdServicio());

        if (prodVerificacionOpt.isEmpty()) {
            return;
        }

        GestoPagoProducto prodVerificacion = prodVerificacionOpt.get();
        GestoPagoSendTxPayload payloadVerificacion = GestoPagoSendTxPayload.builder()
                .idServicio(prodVerificacion.getIdServicio())
                .idProducto(prodVerificacion.getIdProducto())
                .tipoFront(4)
                .telefono(PagoReferenciaPayloadStrategy.TELEFONO_FIJO_SERVICIOS)
                .referencia(calculo.getReferenciaEnviada())
                .horaLocal(horaLocal)
                .montoPago(calculo.getMonto().toPlainString())
                .upc(generarUpcUnico())
                .unidad(unidadConfigurada)
                .build();

        try {
            gestoPagoIntegrationService.enviarTransaccion(payloadVerificacion);
            log.info("Validación previa de referencia (tipoFront 4) exitosa para idServicio={}",
                    productoCatalogo.getIdServicio());
        } catch (GestoPagoProveedorRechazoException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Validación previa de referencia no disponible ({}); continuando con el pago",
                    e.getClass().getSimpleName());
        }
    }

    private String obtenerUnidadNormalizada() {
        if (unidadConfigurada == null || unidadConfigurada.isBlank() || "1".equals(unidadConfigurada.trim())) {
            return "083";
        }
        String limpia = unidadConfigurada.trim();
        if (limpia.length() < 3) {
            return String.format("%3s", limpia).replace(' ', '0');
        }
        return limpia;
    }

    private String generarUpcUnico() {
        String candidato;
        int intentos = 0;
        do {
            StringBuilder sb = new StringBuilder(LONGITUD_UPC);
            for (int i = 0; i < LONGITUD_UPC; i++) {
                sb.append(CARACTERES_UPC.charAt(SECURE_RANDOM.nextInt(CARACTERES_UPC.length())));
            }
            candidato = sb.toString();
            intentos++;
        } while (intentos < 25 && transaccionPagoRepository.existsByUpc(candidato));
        return candidato;
    }
}

