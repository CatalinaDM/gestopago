package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.repositorys.pago.TransaccionPagoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job programado para detectar transacciones que quedaron en estado PENDIENTE
 * (por ejemplo, ante un reinicio abrupto o pérdida de conexión durante sendTx)
 * y pasarlas a EN_REVISION.
 *
 * Proceso de conciliación manual:
 * 1. Consultar las transacciones en estado EN_REVISION en la tabla transacciones_pago.
 * 2. Verificar el estado real en el portal/reporte de GestoPago usando el código único `upc` y `fecha_creacion`.
 * 3. Si el proveedor aplicó el cargo, actualizar a EXITOSA registrando id_tx_proveedor y num_autorizacion.
 * 4. Si el proveedor no registró la operación, actualizar a FALLIDA y acreditar el reembolso (REEMBOLSO_PAGO) en movimientos_cuenta.
 */
@Component
@Slf4j
public class ConciliacionPagosScheduler {

    private final TransaccionPagoRepository transaccionPagoRepository;

    @Value("${gestopago.tx.conciliacion.minutos-umbral:5}")
    private long minutosUmbral = 5L;

    public ConciliacionPagosScheduler(TransaccionPagoRepository transaccionPagoRepository) {
        this.transaccionPagoRepository = transaccionPagoRepository;
    }

    @Scheduled(fixedDelayString = "${gestopago.tx.conciliacion.intervalo-ms:60000}")
    @Transactional
    public void marcarPendientesAntiguasEnRevision() {
        LocalDateTime fechaLimite = LocalDateTime.now().minusMinutes(minutosUmbral > 0 ? minutosUmbral : 5L);
        List<TransaccionPago> pendientesAntiguas = transaccionPagoRepository
                .findByEstadoAndFechaCreacionBefore(EstadoTransaccion.PENDIENTE, fechaLimite);

        if (pendientesAntiguas.isEmpty()) {
            return;
        }

        for (TransaccionPago tx : pendientesAntiguas) {
            tx.setEstado(EstadoTransaccion.EN_REVISION);
            tx.setMensajeRespuesta("Marcada automáticamente EN_REVISION por exceder el tiempo máximo en estado PENDIENTE. Requiere conciliación manual con UPC: " + tx.getUpc());
        }

        transaccionPagoRepository.saveAll(pendientesAntiguas);
        log.warn("Conciliación automática: {} transacciones PENDIENTE antiguas pasaron a EN_REVISION",
                pendientesAntiguas.size());
    }
}

