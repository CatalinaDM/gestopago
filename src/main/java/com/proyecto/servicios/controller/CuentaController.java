package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cuenta.CuentaResponse;
import com.proyecto.servicios.model.cuenta.SaldoCuentaResponse;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponse> obtenerPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerPorNumeroCuenta(numeroCuenta));
    }

    @GetMapping("/{numeroCuenta}/saldo")
    public ResponseEntity<SaldoCuentaResponse> obtenerSaldo(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerSaldo(numeroCuenta));
    }

    @GetMapping("/activas")
    public ResponseEntity<List<CuentaResponse>> obtenerCuentasActivas() {
        return ResponseEntity.ok(cuentaService.obtenerCuentasActivas());
    }
}
