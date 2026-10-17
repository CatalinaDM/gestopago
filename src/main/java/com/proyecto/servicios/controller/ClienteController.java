package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.cliente.ClienteAccionRequest;
import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteConsultaRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Endpoints para registro, consulta unificada, actualización y gestión del ciclo de vida (baja/reactivación) de clientes.")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @Operation(
            summary = "Registrar nuevo cliente",
            description = "Crea un cliente nuevo en el sistema con su domicilio, cuenta bancaria inicial y usuario de acceso. Valida mayoría de edad (>=18 años) y unicidad de CURP, RFC y correo electrónico."
    )
    public ResponseEntity<ClienteResponse> registrarCliente(
            @Valid @RequestBody GenericRequest<ClienteRegistroRequest> request) {
        ClienteResponse response = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/consulta")
    @Operation(
            summary = "Punto único de consulta flexible y paginada (Solo Admin)",
            description = "Permite buscar clientes combinando cualquier criterio opcional en el cuerpo de la petición: por CURP, RFC, email, número de cuenta, texto libre ('filtro'), rango de fechas ('fechaInicio', 'fechaFin'), estado activo ('soloActivos': true/false) y paginación ('pagina' y 'tamano'). Si no se envían filtros, retorna el listado paginado por defecto."
    )
    public ResponseEntity<Page<ClienteResponse>> consultarClientes(
            @Parameter(hidden = true) @RequestAttribute(value = "rol", required = false) Integer rol,
            @Valid @RequestBody(required = false) GenericRequest<ClienteConsultaRequest> request) {

        validarAdmin(rol);
        return ResponseEntity.ok(clienteService.consultar(request));
    }

    @PostMapping("/me")
    @Operation(
            summary = "Obtener perfil del cliente autenticado",
            description = "Retorna el expediente completo del cliente correspondiente al usuario que ha iniciado sesión a través de su token JWT."
    )
    public ResponseEntity<ClienteResponse> obtenerMiPerfil(
            @Parameter(hidden = true) @RequestAttribute(value = "clienteId", required = false) Integer clienteId) {
        return ResponseEntity.ok(clienteService.obtenerPerfil(clienteId));
    }

    @PutMapping
    @Operation(
            summary = "Actualizar información del cliente",
            description = "Permite modificar datos personales, de contacto, domicilio e información laboral. Por regla de negocio, los campos CURP, RFC y Número de Cuenta son inmutables. Solo el Administrador o el propio cliente pueden modificar su registro."
    )
    public ResponseEntity<ClienteResponse> actualizarCliente(
            @Parameter(hidden = true) @RequestAttribute(value = "rol", required = false) Integer rol,
            @Parameter(hidden = true) @RequestAttribute(value = "clienteId", required = false) Integer tokenClienteId,
            @Valid @RequestBody GenericRequest<ClienteActualizaRequest> request) {

        Integer idDelBody = request.getDatos() != null ? request.getDatos().getId() : null;

        if ((rol == null || rol != 1) && (tokenClienteId == null || !tokenClienteId.equals(idDelBody))) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException();
        }

        return ResponseEntity.ok(clienteService.actualizarCliente(request));
    }

    @PostMapping("/estado")
    @Operation(
            summary = "Cambiar estado de cliente: Baja lógica o Reactivación (Solo Admin)",
            description = "Endpoint unificado para modificar el ciclo de vida del cliente mediante una bandera o acción en el cuerpo de la petición ('accion': 'BAJA' o 'REACTIVAR'). La baja lógica desactiva en cascada las cuentas y el acceso del usuario sin eliminar físicamente datos. La reactivación restablece el estado activo."
    )
    public ResponseEntity<GenericResponse> cambiarEstadoCliente(
            @Valid @RequestBody GenericRequest<ClienteAccionRequest> request,
            @Parameter(hidden = true) @RequestAttribute(value = "rol", required = false) Integer rol) {
        validarAdmin(rol);
        return ResponseEntity.ok(clienteService.cambiarEstadoCliente(request));
    }

    private void validarAdmin(Integer rol) {
        if (rol == null || rol != 1) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException();
        }
    }
}
