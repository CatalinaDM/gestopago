package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.cliente.ClienteAccionRequest;
import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteConsultaRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> registrarCliente(
            @Valid @RequestBody GenericRequest<ClienteRegistroRequest> request) {
        ClienteResponse response = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Punto único de consulta flexible para clientes.
     * Permite filtrar por: curp, rfc, email, numeroCuenta, soloActivos, rango de fechas, texto general y paginación.
     */
    @PostMapping("/consulta")
    public ResponseEntity<Page<ClienteResponse>> consultarClientes(
            @RequestAttribute(value = "rol", required = false) Integer rol,
            @Valid @RequestBody(required = false) GenericRequest<ClienteConsultaRequest> request) {

        validarAdmin(rol);
        return ResponseEntity.ok(clienteService.consultar(request));
    }

    @PostMapping("/me")
    public ResponseEntity<ClienteResponse> obtenerMiPerfil(
            @RequestAttribute(value = "clienteId", required = false) Integer clienteId) {
        if (clienteId == null) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException("AUTH-005", "El usuario autenticado no tiene un cliente asociado");
        }
        return ResponseEntity.ok(clienteService.obtenerPerfil(clienteId));
    }

    @PutMapping
    public ResponseEntity<ClienteResponse> actualizarCliente(
            @RequestAttribute(value = "rol", required = false) Integer rol,
            @RequestAttribute(value = "clienteId", required = false) Integer tokenClienteId,
            @Valid @RequestBody GenericRequest<ClienteActualizaRequest> request) {

        Integer idDelBody = request.getDatos() != null ? request.getDatos().getId() : null;

        // Solo Admin o el propio cliente pueden modificar su registro
        if ((rol == null || rol != 1) && (tokenClienteId == null || !tokenClienteId.equals(idDelBody))) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException();
        }

        return ResponseEntity.ok(clienteService.actualizarCliente(request));
    }

    @PostMapping("/baja")
    public ResponseEntity<GenericResponse> darDeBajaCliente(
            @Valid @RequestBody GenericRequest<ClienteAccionRequest> request,
            @RequestAttribute(value = "rol", required = false) Integer rol) {
        validarAdmin(rol);
        clienteService.darDeBajaCliente(request);
        return ResponseEntity.ok(GenericResponse.success("Cliente y servicios asociados dados de baja correctamente"));
    }

    @PostMapping("/reactivar")
    public ResponseEntity<GenericResponse> reactivarCliente(
            @Valid @RequestBody GenericRequest<ClienteAccionRequest> request,
            @RequestAttribute(value = "rol", required = false) Integer rol) {
        validarAdmin(rol);
        clienteService.reactivarCliente(request);
        return ResponseEntity.ok(GenericResponse.success("Cliente y servicios asociados reactivados correctamente"));
    }

    private void validarAdmin(Integer rol) {
        if (rol == null || rol != 1) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException();
        }
    }
}
