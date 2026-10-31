package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.entity.pago.MovimientoCuenta;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.enums.ErrorCode;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.enums.TipoMovimientoCuenta;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxXmlResponse;
import com.proyecto.servicios.model.pago.CalculoOperacionPago;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.pago.MovimientoCuentaRepository;
import com.proyecto.servicios.repositorys.pago.TransaccionPagoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class PagoTransaccionHelper {

    private final CuentaRepository cuentaRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final MovimientoCuentaRepository movimientoCuentaRepository;

    public PagoTransaccionHelper(
            CuentaRepository cuentaRepository,
            TransaccionPagoRepository transaccionPagoRepository,
            MovimientoCuentaRepository movimientoCuentaRepository) {
        this.cuentaRepository = cuentaRepository;
        this.transaccionPagoRepository = transaccionPagoRepository;
        this.movimientoCuentaRepository = movimientoCuentaRepository;
    }

    /**
     * Primera transacción local corta:
     * Bloquea la cuenta con PESSIMISTIC_WRITE, valida titularidad, estado activo y saldo >= totalCobrado,
     * descuenta el saldo, guarda la transacción en estado PENDIENTE y registra el movimiento de cargo.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionPago reservarSaldoYCrearPendiente(
            String numeroCuenta,
            Integer clienteId,
            GestoPagoProducto productoCatalogo,
            CalculoOperacionPago calculo,
            String upc) {

        Cuenta cuenta = cuentaRepository.findByNumeroCuentaForUpdate(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));

        if (clienteId == null || cuenta.getCliente() == null || !clienteId.equals(cuenta.getCliente().getId())) {
            throw new AccesoDenegadoException("AUTH-004",
                    "Acceso denegado: La cuenta bancaria no pertenece al usuario autenticado");
        }

        if (Boolean.FALSE.equals(cuenta.getCliente().getActivo())) {
            throw new AccesoDenegadoException("CLIENTE-INACTIVO", ErrorCode.CLIENTE_INACTIVO.getMessage());
        }

        if (Boolean.FALSE.equals(cuenta.getActivo())) {
            throw new CuentaInactivaException(numeroCuenta);
        }

        BigDecimal saldoActual = cuenta.getSaldo() != null ? cuenta.getSaldo() : BigDecimal.ZERO;
        BigDecimal totalCobrar = calculo.getTotalCobrado().setScale(2, RoundingMode.HALF_UP);

        if (saldoActual.compareTo(totalCobrar) < 0) {
            log.warn("PAGO-SALDO-001: Saldo insuficiente en cuenta {} [requerido={}]", numeroCuenta, totalCobrar);
            throw new SaldoInsuficienteException("Saldo insuficiente en la cuenta para cubrir el monto total de $" + totalCobrar);
        }

        BigDecimal nuevoSaldo = saldoActual.subtract(totalCobrar).setScale(2, RoundingMode.HALF_UP);
        cuenta.setSaldo(nuevoSaldo);
        cuentaRepository.save(cuenta);

        TransaccionPago transaccion = new TransaccionPago();
        transaccion.setCuenta(cuenta);
        transaccion.setIdProducto(productoCatalogo.getIdProducto());
        transaccion.setIdServicio(productoCatalogo.getIdServicio());
        transaccion.setUpc(upc);
        transaccion.setReferenciaEnviada(calculo.getReferenciaEnviada());
        transaccion.setTelefonoEnviado(calculo.getTelefonoEnviado());
        transaccion.setMonto(calculo.getMonto());
        transaccion.setComision(calculo.getComision());
        transaccion.setTotalCobrado(totalCobrar);
        transaccion.setEstado(EstadoTransaccion.PENDIENTE);
        transaccion = transaccionPagoRepository.save(transaccion);

        MovimientoCuenta cargo = new MovimientoCuenta();
        cargo.setCuenta(cuenta);
        cargo.setTransaccionPago(transaccion);
        cargo.setTipoMovimiento(TipoMovimientoCuenta.CARGO_PAGO);
        cargo.setMonto(totalCobrar);
        cargo.setSaldoAnterior(saldoActual);
        cargo.setSaldoPosterior(nuevoSaldo);
        cargo.setDescripcion("Cargo por pago de servicio/recarga UPC: " + upc);
        movimientoCuentaRepository.save(cargo);

        log.info("Reserva de saldo aplicada y transacción PENDIENTE creada [id={}, upc={}, cuenta={}]",
                transaccion.getId(), upc, numeroCuenta);
        return transaccion;
    }

    /**
     * Segunda transacción local corta (caso éxito CODIGO == "01"):
     * Confirma la transacción como EXITOSA y almacena los datos de autorización del proveedor.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionPago confirmarTransaccionExitosa(Integer idTransaccion, GestoPagoSendTxXmlResponse xmlResponse) {
        TransaccionPago transaccion = transaccionPagoRepository.findById(idTransaccion)
                .orElseThrow(() -> new IllegalStateException("Transacción no encontrada ID: " + idTransaccion));

        transaccion.setEstado(EstadoTransaccion.EXITOSA);
        transaccion.setCodigoRespuesta(xmlResponse.obtenerCodigoEfectivo());
        transaccion.setMensajeRespuesta(xmlResponse.obtenerTextoEfectivo());
        transaccion.setIdTxProveedor(xmlResponse.getIdTx());
        transaccion.setNumAutorizacion(xmlResponse.getNumAutorizacion());
        transaccion.setPinProducto(xmlResponse.obtenerPinEfectivo());
        transaccion.setFolio(xmlResponse.obtenerFolioEfectivo());
        transaccion.setLeyenda(xmlResponse.obtenerLeyendaEfectiva());

        transaccion = transaccionPagoRepository.save(transaccion);
        log.info("Transacción confirmada como EXITOSA [id={}, upc={}, idTxProveedor={}]",
                transaccion.getId(), transaccion.getUpc(), transaccion.getIdTxProveedor());
        return transaccion;
    }

    /**
     * Segunda transacción local corta (caso rechazo/error claro del proveedor):
     * Marca la transacción como FALLIDA, devuelve el saldo a la cuenta y registra el movimiento inverso (reembolso).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionPago reversarTransaccionFallida(
            Integer idTransaccion,
            String numeroCuenta,
            String codigoRespuesta,
            String mensajeError) {

        Cuenta cuenta = cuentaRepository.findByNumeroCuentaForUpdate(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));

        TransaccionPago transaccion = transaccionPagoRepository.findById(idTransaccion)
                .orElseThrow(() -> new IllegalStateException("Transacción no encontrada ID: " + idTransaccion));

        if (transaccion.getEstado() != EstadoTransaccion.PENDIENTE) {
            return transaccion;
        }

        BigDecimal saldoAnterior = cuenta.getSaldo() != null ? cuenta.getSaldo() : BigDecimal.ZERO;
        BigDecimal montoReembolso = transaccion.getTotalCobrado();
        BigDecimal saldoPosterior = saldoAnterior.add(montoReembolso).setScale(2, RoundingMode.HALF_UP);

        cuenta.setSaldo(saldoPosterior);
        cuentaRepository.save(cuenta);

        transaccion.setEstado(EstadoTransaccion.FALLIDA);
        transaccion.setCodigoRespuesta(codigoRespuesta);
        transaccion.setMensajeRespuesta(mensajeError);
        transaccion = transaccionPagoRepository.save(transaccion);

        MovimientoCuenta reembolso = new MovimientoCuenta();
        reembolso.setCuenta(cuenta);
        reembolso.setTransaccionPago(transaccion);
        reembolso.setTipoMovimiento(TipoMovimientoCuenta.REEMBOLSO_PAGO);
        reembolso.setMonto(montoReembolso);
        reembolso.setSaldoAnterior(saldoAnterior);
        reembolso.setSaldoPosterior(saldoPosterior);
        reembolso.setDescripcion("Reembolso automático por transacción fallida UPC: " + transaccion.getUpc());
        movimientoCuentaRepository.save(reembolso);

        log.info("Transacción marcada como FALLIDA y reembolso aplicado [id={}, upc={}, cuenta={}]",
                transaccion.getId(), transaccion.getUpc(), numeroCuenta);
        return transaccion;
    }

    /**
     * Segunda transacción local corta (caso timeout o respuesta ambigua):
     * Marca la transacción como EN_REVISION sin reembolso automático para conciliación segura.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransaccionPago marcarEnRevision(Integer idTransaccion, String motivo) {
        TransaccionPago transaccion = transaccionPagoRepository.findById(idTransaccion)
                .orElseThrow(() -> new IllegalStateException("Transacción no encontrada ID: " + idTransaccion));

        if (transaccion.getEstado() == EstadoTransaccion.PENDIENTE) {
            transaccion.setEstado(EstadoTransaccion.EN_REVISION);
            transaccion.setMensajeRespuesta(motivo);
            transaccion = transaccionPagoRepository.save(transaccion);
            log.warn("Transacción marcada como EN_REVISION sin reembolso automático [id={}, upc={}]",
                    transaccion.getId(), transaccion.getUpc());
        }
        return transaccion;
    }
}

