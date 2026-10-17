package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.cuenta.CuentaResponse;
import com.proyecto.servicios.model.cuenta.SaldoCuentaResponse;
import com.proyecto.servicios.service.CuentaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<CuentaResponse>> obtenerMisCuentas(
            @RequestAttribute(value = "clienteId", required = false) Integer clienteId) {
        if (clienteId == null) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException();
        }
        return ResponseEntity.ok(cuentaService.obtenerCuentasPorCliente(clienteId));
    }

    @PostMapping("/consulta")
    public ResponseEntity<CuentaResponse> obtenerPorNumeroCuenta(
            @Valid @RequestBody GenericRequest<com.proyecto.servicios.model.cuenta.CuentaConsultaRequest> request,
            @RequestAttribute(value = "rol", required = false) Integer rol,
            @RequestAttribute(value = "clienteId", required = false) Integer clienteId) {
        String num = request != null && request.getDatos() != null ? request.getDatos().getNumeroCuenta() : null;
        return ResponseEntity.ok(cuentaService.obtenerPorNumeroCuenta(num, rol, clienteId));
    }

    @PostMapping("/saldo")
    public ResponseEntity<SaldoCuentaResponse> obtenerSaldo(
            @Valid @RequestBody GenericRequest<com.proyecto.servicios.model.cuenta.CuentaConsultaRequest> request,
            @RequestAttribute(value = "rol", required = false) Integer rol,
            @RequestAttribute(value = "clienteId", required = false) Integer clienteId) {
        String num = request != null && request.getDatos() != null ? request.getDatos().getNumeroCuenta() : null;
        return ResponseEntity.ok(cuentaService.obtenerSaldo(num, rol, clienteId));
    }

    @PostMapping("/activas")
    public ResponseEntity<List<CuentaResponse>> obtenerCuentasActivas(
            @RequestAttribute(value = "rol", required = false) Integer rol) {
        if (rol == null || rol != 1) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException();
        }
        return ResponseEntity.ok(cuentaService.obtenerCuentasActivas());
    }
}
