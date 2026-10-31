package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.pago.AutorizacionNipResponse;
import com.proyecto.servicios.model.pago.ComprobantePagoResponse;
import com.proyecto.servicios.model.pago.RealizarPagoRequest;
import com.proyecto.servicios.model.pago.SolicitarNipTransaccionRequest;
import com.proyecto.servicios.service.PagoServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagos")
@Tag(name = "Pagos de Servicios y Recargas", description = "Endpoints para autorización con NIP transaccional y ejecución de pagos de servicios, recargas y pines prepago vía GestoPago (sendTx).")
public class PagoController {

    private final PagoServicioService pagoServicioService;

    public PagoController(PagoServicioService pagoServicioService) {
        this.pagoServicioService = pagoServicioService;
    }

    @PostMapping("/autorizar-nip")
    @Operation(
            summary = "Solicitar NIP transaccional para auto-rellenado (SoftToken)",
            description = "Genera el código NIP de autorización transaccional de 4 a 6 dígitos para el usuario autenticado que está por realizar un pago desde su cuenta activa. En base de datos solo se almacena el hash BCrypt."
    )
    public ResponseEntity<AutorizacionNipResponse> solicitarNipParaPago(
            @Valid @RequestBody GenericRequest<SolicitarNipTransaccionRequest> request,
            @Parameter(hidden = true) @RequestAttribute(value = "usuarioId", required = false) Integer usuarioId,
            @Parameter(hidden = true) @RequestAttribute(value = "clienteId", required = false) Integer clienteId) {
        return ResponseEntity.ok(pagoServicioService.solicitarNipParaPago(request, usuarioId, clienteId));
    }

    @PostMapping("/servicios")
    @Operation(
            summary = "Realizar pago de servicio, recarga o compra de PIN",
            description = "Ejecuta el flujo completo de pago (reservar saldo -> cifrar AES-256 -> invocar sendTx GestoPago -> confirmar o reembolsar) validando titularidad de la cuenta, formato del producto y NIP transaccional con BCrypt."
    )
    public ResponseEntity<ComprobantePagoResponse> realizarPagoServicio(
            @Valid @RequestBody GenericRequest<RealizarPagoRequest> request,
            @Parameter(hidden = true) @RequestAttribute(value = "usuarioId", required = false) Integer usuarioId,
            @Parameter(hidden = true) @RequestAttribute(value = "clienteId", required = false) Integer clienteId) {
        return ResponseEntity.ok(pagoServicioService.realizarPago(request, usuarioId, clienteId));
    }
}

