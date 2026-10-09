package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        ClienteResponse response = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> obtenerClientes(
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String rfc,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String numeroCuenta) {

        if (filtro != null && !filtro.isBlank()) {
            return ResponseEntity.ok(clienteService.buscarClientes(filtro));
        }

        if ((curp != null && !curp.isBlank())
                || (rfc != null && !rfc.isBlank())
                || (email != null && !email.isBlank())
                || (numeroCuenta != null && !numeroCuenta.isBlank())) {
            return ResponseEntity.ok(clienteService.buscarPorCriterios(curp, rfc, email, numeroCuenta));
        }

        return ResponseEntity.ok(clienteService.obtenerTodos());
    }

    @GetMapping("/paginados")
    public ResponseEntity<Page<ClienteResponse>> obtenerClientesPaginados(
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            Pageable pageable) {

        if (filtro != null && !filtro.isBlank()) {
            return ResponseEntity.ok(clienteService.buscarClientesPaginados(filtro, pageable));
        }
        if (soloActivos) {
            return ResponseEntity.ok(clienteService.obtenerActivosPaginados(pageable));
        }
        return ResponseEntity.ok(clienteService.obtenerPaginados(pageable));
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ClienteResponse>> obtenerClientesActivos() {
        return ResponseEntity.ok(clienteService.obtenerActivos());
    }

    @GetMapping("/me")
    public ResponseEntity<ClienteResponse> obtenerMiPerfil(
            @RequestAttribute(value = "clienteId", required = false) Integer clienteId) {
        return ResponseEntity.ok(clienteService.obtenerPerfil(clienteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    @GetMapping("/curp/{curp}")
    public ResponseEntity<List<ClienteResponse>> obtenerPorCurp(@PathVariable String curp) {
        return ResponseEntity.ok(clienteService.obtenerPorCurp(curp));
    }

    @GetMapping("/rfc/{rfc}")
    public ResponseEntity<List<ClienteResponse>> obtenerPorRfc(@PathVariable String rfc) {
        return ResponseEntity.ok(clienteService.obtenerPorRfc(rfc));
    }

    @GetMapping("/correo/{correo}")
    public ResponseEntity<List<ClienteResponse>> obtenerPorCorreo(@PathVariable String correo) {
        return ResponseEntity.ok(clienteService.obtenerPorCorreo(correo));
    }

    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<ClienteResponse> obtenerPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(clienteService.obtenerPorNumeroCuenta(numeroCuenta));
    }

    @GetMapping("/rango-fechas")
    public ResponseEntity<List<ClienteResponse>> obtenerPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(clienteService.obtenerPorRangoFechas(fechaInicio, fechaFin));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ClienteResponse>> buscarClientes(@RequestParam String filtro) {
        return ResponseEntity.ok(clienteService.buscarClientes(filtro));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> actualizarCliente(
            @PathVariable Integer id,
            @Valid @RequestBody ClienteActualizaRequest request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse> darDeBajaCliente(@PathVariable Integer id) {
        clienteService.darDeBajaCliente(id);
        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje("Cliente y servicios asociados dados de baja correctamente");
        return ResponseEntity.ok(response);
    }
}
