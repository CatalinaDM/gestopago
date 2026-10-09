package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cuenta.CuentaResponse;
import com.proyecto.servicios.model.cuenta.SaldoCuentaResponse;
import com.proyecto.servicios.service.CuentaService;
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
            throw new com.proyecto.servicios.exception.AccesoDenegadoException(
                    "AUTH-005", "El usuario autenticado no tiene un cliente asociado");
        }
        return ResponseEntity.ok(cuentaService.obtenerCuentasPorCliente(clienteId));
    }

    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponse> obtenerPorNumeroCuenta(
            @PathVariable String numeroCuenta,
            @RequestAttribute(value = "rol", required = false) Integer rol,
            @RequestAttribute(value = "clienteId", required = false) Integer clienteId) {
        return ResponseEntity.ok(cuentaService.obtenerPorNumeroCuenta(numeroCuenta, rol, clienteId));
    }

    @GetMapping("/{numeroCuenta}/saldo")
    public ResponseEntity<SaldoCuentaResponse> obtenerSaldo(
            @PathVariable String numeroCuenta,
            @RequestAttribute(value = "rol", required = false) Integer rol,
            @RequestAttribute(value = "clienteId", required = false) Integer clienteId) {
        return ResponseEntity.ok(cuentaService.obtenerSaldo(numeroCuenta, rol, clienteId));
    }

    @GetMapping("/activas")
    public ResponseEntity<List<CuentaResponse>> obtenerCuentasActivas(
            @RequestAttribute(value = "rol", required = false) Integer rol) {
        if (rol == null || rol != 1) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException(
                    "AUTH-004", "Acceso denegado: Solo los administradores pueden consultar la lista global de cuentas");
        }
        return ResponseEntity.ok(cuentaService.obtenerCuentasActivas());
    }
}
