package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.cuenta.CuentaResponse;
import com.proyecto.servicios.model.cuenta.SaldoCuentaResponse;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cuentas")
@Tag(name = "Cuentas Bancarias", description = "Endpoints para consulta de cuentas, verificación de saldos y listado de cuentas activas.")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping("/me")
    @Operation(
            summary = "Obtener cuentas del cliente autenticado",
            description = "Devuelve el listado de todas las cuentas bancarias asociadas al cliente que ha iniciado sesión."
    )
    public ResponseEntity<List<CuentaResponse>> obtenerMisCuentas(
            @Parameter(hidden = true) @RequestAttribute(value = "clienteId", required = false) Integer clienteId) {
        if (clienteId == null) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException();
        }
        return ResponseEntity.ok(cuentaService.obtenerCuentasPorCliente(clienteId));
    }

    @PostMapping("/consulta")
    @Operation(
            summary = "Consultar detalle de una cuenta bancaria",
            description = "Permite consultar el detalle de una cuenta bancaria por su número de cuenta (10 dígitos). El Administrador puede consultar cualquier cuenta; un cliente solo puede consultar cuentas de su propiedad."
    )
    public ResponseEntity<CuentaResponse> obtenerPorNumeroCuenta(
            @Valid @RequestBody GenericRequest<com.proyecto.servicios.model.cuenta.CuentaConsultaRequest> request,
            @Parameter(hidden = true) @RequestAttribute(value = "rol", required = false) Integer rol,
            @Parameter(hidden = true) @RequestAttribute(value = "clienteId", required = false) Integer clienteId) {
        String num = request != null && request.getDatos() != null ? request.getDatos().getNumeroCuenta() : null;
        return ResponseEntity.ok(cuentaService.obtenerPorNumeroCuenta(num, rol, clienteId));
    }

    @PostMapping("/saldo")
    @Operation(
            summary = "Consultar saldo disponible de una cuenta",
            description = "Devuelve el saldo actual de una cuenta bancaria indicada en el body por su número de cuenta. Aplica control de acceso RBAC por titularidad."
    )
    public ResponseEntity<SaldoCuentaResponse> obtenerSaldo(
            @Valid @RequestBody GenericRequest<com.proyecto.servicios.model.cuenta.CuentaConsultaRequest> request,
            @Parameter(hidden = true) @RequestAttribute(value = "rol", required = false) Integer rol,
            @Parameter(hidden = true) @RequestAttribute(value = "clienteId", required = false) Integer clienteId) {
        String num = request != null && request.getDatos() != null ? request.getDatos().getNumeroCuenta() : null;
        return ResponseEntity.ok(cuentaService.obtenerSaldo(num, rol, clienteId));
    }

    @PostMapping("/activas")
    @Operation(
            summary = "Listar todas las cuentas activas (Solo Admin)",
            description = "Retorna el catálogo completo de cuentas activas del sistema. Requiere rol de Administrador."
    )
    public ResponseEntity<List<CuentaResponse>> obtenerCuentasActivas(
            @Parameter(hidden = true) @RequestAttribute(value = "rol", required = false) Integer rol) {
        if (rol == null || rol != 1) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException();
        }
        return ResponseEntity.ok(cuentaService.obtenerCuentasActivas());
    }
}
