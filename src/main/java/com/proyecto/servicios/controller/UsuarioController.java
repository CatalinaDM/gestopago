package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.usuario.ActualizarPasswordRequest;
import com.proyecto.servicios.model.usuario.UsuarioConsultaRequest;
import com.proyecto.servicios.model.usuario.UsuarioDesbloqueoRequest;
import com.proyecto.servicios.model.usuario.UsuarioResponse;
import com.proyecto.servicios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "Endpoints para administración de usuarios del sistema, cambio de contraseña y desbloqueo de cuentas.")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/consulta")
    @Operation(
            summary = "Consultar usuario por ID (Solo Admin)",
            description = "Devuelve el perfil del usuario (correo, rol, estado activo, intentos fallidos) indicando su ID en el cuerpo de la petición. Requiere rol de Administrador."
    )
    public ResponseEntity<UsuarioResponse> obtenerPorId(
            @Valid @RequestBody GenericRequest<UsuarioConsultaRequest> request,
            @Parameter(hidden = true) @RequestAttribute(value = "rol", required = false) Integer rol) {
        if (rol == null || rol != 1) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException();
        }
        return ResponseEntity.ok(usuarioService.obtenerPorId(request));
    }

    @PutMapping("/actualizar-password")
    @Operation(
            summary = "Actualizar contraseña de usuario",
            description = "Permite cambiar la contraseña de acceso proporcionando la contraseña actual y la nueva contraseña. Valida que la nueva contraseña sea distinta de la actual y que cumpla los requisitos de seguridad. Solo el Administrador o el propio usuario dueño de la cuenta pueden ejecutar esta acción."
    )
    public ResponseEntity<GenericResponse> actualizarPassword(
            @Valid @RequestBody GenericRequest<ActualizarPasswordRequest> request,
            @Parameter(hidden = true) @RequestAttribute(value = "rol", required = false) Integer rol,
            @Parameter(hidden = true) @RequestAttribute(value = "usuarioId", required = false) Integer usuarioId) {

        Integer idTarget = request.getDatos() != null ? request.getDatos().getId() : null;
        if ((rol == null || rol != 1) && (usuarioId != null && !usuarioId.equals(idTarget))) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException();
        }

        GenericResponse response = usuarioService.actualizarPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/desbloquear")
    @Operation(
            summary = "Desbloquear usuario suspendido (Solo Admin)",
            description = "Reactiva a un usuario que ha sido bloqueado tras alcanzar el número máximo de intentos fallidos de autenticación (3 intentos). Restablece los intentos a 0 y activa la cuenta. Requiere rol de Administrador."
    )
    public ResponseEntity<GenericResponse> desbloquearUsuario(
            @Valid @RequestBody GenericRequest<UsuarioDesbloqueoRequest> request,
            @Parameter(hidden = true) @RequestAttribute(value = "rol", required = false) Integer rol) {

        if (rol == null || rol != 1) {
            throw new com.proyecto.servicios.exception.AccesoDenegadoException();
        }

        return ResponseEntity.ok(usuarioService.desbloquearUsuario(request));
    }
}
